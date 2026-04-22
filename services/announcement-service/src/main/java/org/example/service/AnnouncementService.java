package org.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.exception.BusinessException;
import org.example.dto.AnnouncementAttachmentRequest;
import org.example.dto.AnnouncementRequest;
import org.example.entity.ActivityProjection;
import org.example.entity.Announcement;
import org.example.entity.AnnouncementActivity;
import org.example.entity.AnnouncementAttachment;
import org.example.mapper.ActivityProjectionMapper;
import org.example.mapper.AnnouncementActivityMapper;
import org.example.mapper.AnnouncementAttachmentMapper;
import org.example.mapper.AnnouncementMapper;
import org.example.vo.AnnouncementActivityVO;
import org.example.vo.AnnouncementAttachmentVO;
import org.example.vo.AnnouncementVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class AnnouncementService {

    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_OFFLINE = "OFFLINE";

    private final AnnouncementMapper announcementMapper;
    private final AnnouncementActivityMapper announcementActivityMapper;
    private final AnnouncementAttachmentMapper announcementAttachmentMapper;
    private final ActivityProjectionMapper activityProjectionMapper;
    private final MinioStorageService minioStorageService;

    public AnnouncementService(AnnouncementMapper announcementMapper,
                               AnnouncementActivityMapper announcementActivityMapper,
                               AnnouncementAttachmentMapper announcementAttachmentMapper,
                               ActivityProjectionMapper activityProjectionMapper,
                               MinioStorageService minioStorageService) {
        this.announcementMapper = announcementMapper;
        this.announcementActivityMapper = announcementActivityMapper;
        this.announcementAttachmentMapper = announcementAttachmentMapper;
        this.activityProjectionMapper = activityProjectionMapper;
        this.minioStorageService = minioStorageService;
    }

    public List<AnnouncementVO> listHomeAnnouncements(Integer limit) {
        int pageSize = normalizeLimit(limit);
        LambdaQueryWrapper<Announcement> wrapper = basePublishedWrapper()
                .last("LIMIT " + pageSize);
        return toVOList(announcementMapper.selectList(wrapper));
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
        IPage<AnnouncementVO> voPage = result.convert(this::toBaseVO);
        enrichAnnouncements(voPage.getRecords());
        return voPage;
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
        IPage<AnnouncementVO> voPage = result.convert(this::toBaseVO);
        enrichAnnouncements(voPage.getRecords());
        return voPage;
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
        List<Long> activityIds = normalizeActivityIds(request);
        validateLinkedActivities(activityIds);
        Announcement announcement = new Announcement();
        applyRequest(announcement, request, activityIds);
        announcement.setPublisherId(publisherId);
        if (!StringUtils.hasText(announcement.getStatus())) {
            announcement.setStatus(STATUS_PUBLISHED);
        }
        if (STATUS_PUBLISHED.equals(announcement.getStatus())) {
            announcement.setPublishTime(LocalDateTime.now());
        }
        announcementMapper.insert(announcement);
        syncAnnouncementActivities(announcement.getId(), activityIds);
        syncAnnouncementAttachments(announcement.getId(), normalizeAttachments(request));
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateAnnouncement(Long id, AnnouncementRequest request) {
        validateRequest(request);
        List<Long> activityIds = normalizeActivityIds(request);
        Announcement existing = announcementMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("Announcement not found");
        }
        validateLinkedActivities(activityIds);

        List<String> oldImageKeys = splitImageKeys(existing.getImageKey());
        List<String> oldAttachmentKeys = listAttachmentKeys(id);
        List<AttachmentInput> currentAttachments = normalizeAttachments(request);
        String oldStatus = existing.getStatus();
        applyRequest(existing, request, activityIds);
        if (STATUS_PUBLISHED.equals(existing.getStatus()) && !STATUS_PUBLISHED.equals(oldStatus)) {
            existing.setPublishTime(LocalDateTime.now());
        }
        announcementMapper.updateById(existing);
        syncAnnouncementActivities(id, activityIds);
        syncAnnouncementAttachments(id, currentAttachments);
        deleteRemovedImages(oldImageKeys, splitImageKeys(existing.getImageKey()));
        deleteRemovedObjects(oldAttachmentKeys, currentAttachments.stream().map(AttachmentInput::attachmentKey).toList());
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
        List<String> attachmentKeys = listAttachmentKeys(id);
        announcementMapper.deleteById(id);
        announcementActivityMapper.delete(new LambdaQueryWrapper<AnnouncementActivity>()
                .eq(AnnouncementActivity::getAnnouncementId, id));
        announcementAttachmentMapper.delete(new LambdaQueryWrapper<AnnouncementAttachment>()
                .eq(AnnouncementAttachment::getAnnouncementId, id));
        for (String imageKey : splitImageKeys(announcement.getImageKey())) {
            minioStorageService.deleteObjectQuietly(imageKey);
        }
        for (String attachmentKey : attachmentKeys) {
            minioStorageService.deleteObjectQuietly(attachmentKey);
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

    private void applyRequest(Announcement announcement, AnnouncementRequest request, List<Long> activityIds) {
        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setImageKey(joinImageKeys(normalizeImageKeys(request)));
        announcement.setActivityId(activityIds.isEmpty() ? null : activityIds.get(0));
        announcement.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : STATUS_PUBLISHED);
        announcement.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
    }

    private AnnouncementVO toVO(Announcement announcement) {
        AnnouncementVO vo = toBaseVO(announcement);
        enrichAnnouncements(List.of(vo));
        return vo;
    }

    private List<AnnouncementVO> toVOList(List<Announcement> announcements) {
        if (announcements == null || announcements.isEmpty()) {
            return Collections.emptyList();
        }
        List<AnnouncementVO> vos = announcements.stream().map(this::toBaseVO).toList();
        enrichAnnouncements(vos);
        return vos;
    }

    private AnnouncementVO toBaseVO(Announcement announcement) {
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

    private void enrichAnnouncements(List<AnnouncementVO> announcements) {
        if (announcements == null || announcements.isEmpty()) {
            return;
        }

        List<Long> announcementIds = announcements.stream().map(AnnouncementVO::getId).toList();
        Map<Long, AnnouncementVO> announcementMap = new HashMap<>();
        for (AnnouncementVO announcement : announcements) {
            announcementMap.put(announcement.getId(), announcement);
            announcement.setActivityIds(new ArrayList<>());
            announcement.setActivities(new ArrayList<>());
            announcement.setAttachments(new ArrayList<>());
        }

        Map<Long, LinkedHashSet<Long>> activityIdsByAnnouncement = new HashMap<>();
        List<AnnouncementActivity> links = announcementActivityMapper.selectList(
                new LambdaQueryWrapper<AnnouncementActivity>()
                        .in(AnnouncementActivity::getAnnouncementId, announcementIds)
                        .orderByAsc(AnnouncementActivity::getId)
        );
        for (AnnouncementActivity link : links) {
            activityIdsByAnnouncement
                    .computeIfAbsent(link.getAnnouncementId(), key -> new LinkedHashSet<>())
                    .add(link.getActivityId());
        }
        for (AnnouncementVO announcement : announcements) {
            if (!activityIdsByAnnouncement.containsKey(announcement.getId()) && announcement.getActivityId() != null) {
                activityIdsByAnnouncement
                        .computeIfAbsent(announcement.getId(), key -> new LinkedHashSet<>())
                        .add(announcement.getActivityId());
            }
        }
        enrichActivities(announcements, activityIdsByAnnouncement);

        List<AnnouncementAttachment> attachments = announcementAttachmentMapper.selectList(
                new LambdaQueryWrapper<AnnouncementAttachment>()
                        .in(AnnouncementAttachment::getAnnouncementId, announcementIds)
                        .orderByAsc(AnnouncementAttachment::getAnnouncementId)
                        .orderByAsc(AnnouncementAttachment::getSortOrder)
                        .orderByAsc(AnnouncementAttachment::getId)
        );
        for (AnnouncementAttachment attachment : attachments) {
            AnnouncementVO announcement = announcementMap.get(attachment.getAnnouncementId());
            if (announcement == null) {
                continue;
            }
            announcement.getAttachments().add(toAttachmentVO(attachment));
        }
    }

    private void enrichActivities(List<AnnouncementVO> announcements,
                                  Map<Long, LinkedHashSet<Long>> activityIdsByAnnouncement) {
        LinkedHashSet<Long> allActivityIds = new LinkedHashSet<>();
        for (Set<Long> ids : activityIdsByAnnouncement.values()) {
            allActivityIds.addAll(ids);
        }
        if (allActivityIds.isEmpty()) {
            return;
        }

        Map<Long, ActivityProjection> activityMap = new HashMap<>();
        for (ActivityProjection activity : activityProjectionMapper.selectBatchIds(allActivityIds)) {
            activityMap.put(activity.getId(), activity);
        }

        for (AnnouncementVO announcement : announcements) {
            LinkedHashSet<Long> ids = activityIdsByAnnouncement.getOrDefault(announcement.getId(), new LinkedHashSet<>());
            announcement.setActivityIds(new ArrayList<>(ids));
            List<AnnouncementActivityVO> activityVOS = new ArrayList<>();
            for (Long activityId : ids) {
                ActivityProjection activity = activityMap.get(activityId);
                if (activity == null) {
                    continue;
                }
                AnnouncementActivityVO vo = new AnnouncementActivityVO();
                BeanUtils.copyProperties(activity, vo);
                activityVOS.add(vo);
            }
            announcement.setActivities(activityVOS);
        }
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

    private List<Long> normalizeActivityIds(AnnouncementRequest request) {
        List<Long> rawIds = request.getActivityIds();
        if ((rawIds == null || rawIds.isEmpty()) && request.getActivityId() != null) {
            rawIds = List.of(request.getActivityId());
        }
        if (rawIds == null || rawIds.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<Long> normalized = new LinkedHashSet<>();
        for (Long id : rawIds) {
            if (id != null && id > 0) {
                normalized.add(id);
            }
        }
        return new ArrayList<>(normalized);
    }

    private void validateLinkedActivities(List<Long> activityIds) {
        if (activityIds == null || activityIds.isEmpty()) {
            return;
        }
        List<ActivityProjection> projections = activityProjectionMapper.selectBatchIds(activityIds);
        Map<Long, ActivityProjection> projectionMap = new HashMap<>();
        for (ActivityProjection projection : projections) {
            if (projection != null && projection.getId() != null) {
                projectionMap.put(projection.getId(), projection);
            }
        }
        for (Long activityId : activityIds) {
            ActivityProjection projection = projectionMap.get(activityId);
            if (projection == null) {
                throw new BusinessException("Linked activity does not exist: " + activityId);
            }
            if ("CANCELLED".equals(projection.getStatus())) {
                throw new BusinessException("Cancelled activities cannot be linked to announcements: " + activityId);
            }
        }
    }

    private List<AttachmentInput> normalizeAttachments(AnnouncementRequest request) {
        List<AnnouncementAttachmentRequest> rawAttachments = request.getAttachments();
        if (rawAttachments == null || rawAttachments.isEmpty()) {
            return Collections.emptyList();
        }

        LinkedHashSet<String> seenKeys = new LinkedHashSet<>();
        List<AttachmentInput> normalized = new ArrayList<>();
        for (AnnouncementAttachmentRequest attachment : rawAttachments) {
            if (attachment == null || !StringUtils.hasText(attachment.getAttachmentKey())) {
                continue;
            }
            String attachmentKey = attachment.getAttachmentKey().trim();
            if (!seenKeys.add(attachmentKey)) {
                continue;
            }
            String fileName = StringUtils.hasText(attachment.getFileName())
                    ? attachment.getFileName().trim()
                    : attachmentKey;
            String contentType = StringUtils.hasText(attachment.getContentType())
                    ? attachment.getContentType().trim()
                    : null;
            normalized.add(new AttachmentInput(
                    attachmentKey,
                    fileName,
                    contentType,
                    attachment.getFileSize() == null ? 0L : Math.max(0L, attachment.getFileSize())
            ));
        }
        return normalized;
    }

    private void syncAnnouncementActivities(Long announcementId, List<Long> activityIds) {
        announcementActivityMapper.delete(new LambdaQueryWrapper<AnnouncementActivity>()
                .eq(AnnouncementActivity::getAnnouncementId, announcementId));
        if (activityIds == null || activityIds.isEmpty()) {
            return;
        }
        for (Long activityId : activityIds) {
            AnnouncementActivity link = new AnnouncementActivity();
            link.setAnnouncementId(announcementId);
            link.setActivityId(activityId);
            announcementActivityMapper.insert(link);
        }
    }

    private void syncAnnouncementAttachments(Long announcementId, List<AttachmentInput> attachments) {
        announcementAttachmentMapper.delete(new LambdaQueryWrapper<AnnouncementAttachment>()
                .eq(AnnouncementAttachment::getAnnouncementId, announcementId));
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (int i = 0; i < attachments.size(); i++) {
            AttachmentInput input = attachments.get(i);
            AnnouncementAttachment attachment = new AnnouncementAttachment();
            attachment.setAnnouncementId(announcementId);
            attachment.setObjectKey(input.attachmentKey());
            attachment.setOriginalName(input.fileName());
            attachment.setContentType(input.contentType());
            attachment.setFileSize(input.fileSize());
            attachment.setSortOrder(i);
            announcementAttachmentMapper.insert(attachment);
        }
    }

    private List<String> listAttachmentKeys(Long announcementId) {
        List<AnnouncementAttachment> attachments = announcementAttachmentMapper.selectList(
                new LambdaQueryWrapper<AnnouncementAttachment>()
                        .eq(AnnouncementAttachment::getAnnouncementId, announcementId)
        );
        if (attachments == null || attachments.isEmpty()) {
            return Collections.emptyList();
        }
        return attachments.stream()
                .map(AnnouncementAttachment::getObjectKey)
                .filter(StringUtils::hasText)
                .toList();
    }

    private AnnouncementAttachmentVO toAttachmentVO(AnnouncementAttachment attachment) {
        AnnouncementAttachmentVO vo = new AnnouncementAttachmentVO();
        vo.setAttachmentKey(attachment.getObjectKey());
        vo.setFileName(attachment.getOriginalName());
        vo.setContentType(attachment.getContentType());
        vo.setFileSize(attachment.getFileSize());
        vo.setUrl(minioStorageService.buildAnnouncementAttachmentUrl(
                attachment.getObjectKey(),
                attachment.getOriginalName()
        ));
        return vo;
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
        deleteRemovedObjects(oldImageKeys, currentImageKeys);
    }

    private void deleteRemovedObjects(List<String> oldObjectKeys, List<String> currentObjectKeys) {
        Set<String> current = new LinkedHashSet<>(currentObjectKeys);
        for (String oldObjectKey : oldObjectKeys) {
            if (!current.contains(oldObjectKey)) {
                minioStorageService.deleteObjectQuietly(oldObjectKey);
            }
        }
    }

    private record AttachmentInput(
            String attachmentKey,
            String fileName,
            String contentType,
            Long fileSize
    ) {
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
