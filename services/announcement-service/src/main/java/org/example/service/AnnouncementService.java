package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.exception.BusinessException;
import org.example.dto.AnnouncementRequest;
import org.example.entity.Announcement;
import org.example.mapper.AnnouncementMapper;
import org.example.vo.AnnouncementVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AnnouncementService {

    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_OFFLINE = "OFFLINE";

    private final AnnouncementMapper announcementMapper;
    private final MinioStorageService minioStorageService;

    public AnnouncementService(AnnouncementMapper announcementMapper,
                               MinioStorageService minioStorageService) {
        this.announcementMapper = announcementMapper;
        this.minioStorageService = minioStorageService;
    }

    public List<AnnouncementVO> listHomeAnnouncements(Integer limit) {
        int pageSize = normalizeLimit(limit);
        LambdaQueryWrapper<Announcement> wrapper = basePublishedWrapper()
                .last("LIMIT " + pageSize);
        return announcementMapper.selectList(wrapper).stream().map(this::toVO).toList();
    }

    public IPage<AnnouncementVO> listPublishedAnnouncements(Integer page, Integer size) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        long total = announcementMapper.selectCount(basePublishedWrapper());
        List<Announcement> records = announcementMapper.selectList(
                basePublishedWrapper().last(limitClause(normalizedPage, normalizedSize))
        );
        Page<Announcement> result = new Page<>(normalizedPage, normalizedSize, total);
        result.setRecords(records);
        return result.convert(this::toVO);
    }

    public IPage<AnnouncementVO> listAdminAnnouncements(Integer page, Integer size, String status) {
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        long total = announcementMapper.selectCount(adminListWrapper(status));
        List<Announcement> records = announcementMapper.selectList(
                adminListWrapper(status).last(limitClause(normalizedPage, normalizedSize))
        );
        Page<Announcement> result = new Page<>(normalizedPage, normalizedSize, total);
        result.setRecords(records);
        return result.convert(this::toVO);
    }

    public AnnouncementVO getPublishedDetail(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null || !STATUS_PUBLISHED.equals(announcement.getStatus())) {
            throw new BusinessException("Announcement not found");
        }
        return toVO(announcement);
    }

    public AnnouncementVO getAdminDetail(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("Announcement not found");
        }
        return toVO(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createAnnouncement(AnnouncementRequest request, Long publisherId) {
        validateRequest(request);
        Announcement announcement = new Announcement();
        applyRequest(announcement, request);
        announcement.setPublisherId(publisherId);
        if (!StringUtils.hasText(announcement.getStatus())) {
            announcement.setStatus(STATUS_PUBLISHED);
        }
        if (STATUS_PUBLISHED.equals(announcement.getStatus())) {
            announcement.setPublishTime(LocalDateTime.now());
        }
        announcementMapper.insert(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAnnouncement(Long id, AnnouncementRequest request) {
        validateRequest(request);
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Announcement not found");
        }

        List<String> oldImageKeys = splitImageKeys(existing.getImageKey());
        String oldStatus = existing.getStatus();
        applyRequest(existing, request);
        if (STATUS_PUBLISHED.equals(existing.getStatus()) && !STATUS_PUBLISHED.equals(oldStatus)) {
            existing.setPublishTime(LocalDateTime.now());
        }
        announcementMapper.updateById(existing);
        deleteRemovedImages(oldImageKeys, splitImageKeys(existing.getImageKey()));
    }

    @Transactional(rollbackFor = Exception.class)
    public void publishAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("Announcement not found");
        }
        announcement.setStatus(STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void offlineAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("Announcement not found");
        }
        announcement.setStatus(STATUS_OFFLINE);
        announcementMapper.updateById(announcement);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException("Announcement not found");
        }
        announcementMapper.deleteById(id);
        for (String imageKey : splitImageKeys(announcement.getImageKey())) {
            minioStorageService.deleteObjectQuietly(imageKey);
        }
    }

    private LambdaQueryWrapper<Announcement> basePublishedWrapper() {
        return new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, STATUS_PUBLISHED)
                .orderByDesc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getId);
    }

    private LambdaQueryWrapper<Announcement> adminListWrapper(String status) {
        LambdaQueryWrapper<Announcement> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(status)) {
            wrapper.eq(Announcement::getStatus, status);
        }
        return wrapper.orderByDesc(Announcement::getSortOrder)
                .orderByDesc(Announcement::getPublishTime)
                .orderByDesc(Announcement::getUpdateTime)
                .orderByDesc(Announcement::getId);
    }

    private void validateRequest(AnnouncementRequest request) {
        if (request == null) {
            throw new BusinessException("Announcement request cannot be empty");
        }
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException("Announcement title cannot be empty");
        }
        if (!StringUtils.hasText(request.getContent())) {
            throw new BusinessException("Announcement content cannot be empty");
        }
        if (StringUtils.hasText(request.getStatus())
                && !STATUS_PUBLISHED.equals(request.getStatus())
                && !STATUS_OFFLINE.equals(request.getStatus())) {
            throw new BusinessException("Unsupported announcement status");
        }
    }

    private void applyRequest(Announcement announcement, AnnouncementRequest request) {
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setImageKey(joinImageKeys(normalizeImageKeys(request)));
        announcement.setActivityId(request.getActivityId());
        announcement.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_PUBLISHED);
        announcement.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private AnnouncementVO toVO(Announcement announcement) {
        AnnouncementVO vo = new AnnouncementVO();
        BeanUtils.copyProperties(announcement, vo);
        List<String> imageKeys = splitImageKeys(announcement.getImageKey());
        List<String> imageUrls = buildImageUrls(imageKeys);
        vo.setImageKeys(imageKeys);
        vo.setImageUrls(imageUrls);
        vo.setImageKey(imageKeys.isEmpty() ? null : imageKeys.get(0));
        vo.setImageUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
        return vo;
    }

    private List<String> normalizeImageKeys(AnnouncementRequest request) {
        List<String> rawKeys = request.getImageKeys();
        if (rawKeys == null || rawKeys.isEmpty()) {
            rawKeys = StringUtils.hasText(request.getImageKey())
                    ? List.of(request.getImageKey())
                    : Collections.emptyList();
        }

        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String key : rawKeys) {
            if (StringUtils.hasText(key)) {
                normalized.add(key.trim());
            }
        }
        return new ArrayList<>(normalized);
    }

    private List<String> splitImageKeys(String rawImageKeys) {
        if (!StringUtils.hasText(rawImageKeys)) {
            return Collections.emptyList();
        }
        LinkedHashSet<String> keys = new LinkedHashSet<>();
        for (String part : rawImageKeys.split(",")) {
            if (StringUtils.hasText(part)) {
                keys.add(part.trim());
            }
        }
        return new ArrayList<>(keys);
    }

    private String joinImageKeys(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return null;
        }
        return String.join(",", imageKeys);
    }

    private List<String> buildImageUrls(List<String> imageKeys) {
        if (imageKeys == null || imageKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return imageKeys.stream()
                .map(minioStorageService::buildAnnouncementImageUrl)
                .filter(StringUtils::hasText)
                .toList();
    }

    private void deleteRemovedImages(List<String> oldImageKeys, List<String> currentImageKeys) {
        Set<String> current = new LinkedHashSet<>(currentImageKeys);
        for (String oldImageKey : oldImageKeys) {
            if (!current.contains(oldImageKey)) {
                minioStorageService.deleteObjectQuietly(oldImageKey);
            }
        }
    }

    private int normalizePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size < 1) {
            return 10;
        }
        return Math.min(size, 100);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return 5;
        }
        return Math.min(limit, 20);
    }

    private String limitClause(int page, int size) {
        int offset = (page - 1) * size;
        return "LIMIT " + size + " OFFSET " + offset;
    }
}
