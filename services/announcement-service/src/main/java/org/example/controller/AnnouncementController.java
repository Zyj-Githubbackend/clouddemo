package org.example.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import org.example.common.result.Result;
import org.example.dto.AnnouncementRequest;
import org.example.service.AnnouncementService;
import org.example.service.MinioStorageService;
import org.example.vo.AnnouncementImageUploadVO;
import org.example.vo.AnnouncementVO;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/announcement")
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final MinioStorageService minioStorageService;

    public AnnouncementController(AnnouncementService announcementService,
                                  MinioStorageService minioStorageService) {
        this.announcementService = announcementService;
        this.minioStorageService = minioStorageService;
    }

    @GetMapping("/home")
    public Result<List<AnnouncementVO>> listHomeAnnouncements(
            @RequestParam(defaultValue = "5") Integer limit) {
        return Result.success(announcementService.listHomeAnnouncements(limit));
    }

    @GetMapping("/list")
    public Result<IPage<AnnouncementVO>> listPublishedAnnouncements(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(announcementService.listPublishedAnnouncements(page, size));
    }

    @GetMapping("/{id}")
    public Result<AnnouncementVO> getPublishedDetail(@PathVariable Long id) {
        return Result.success(announcementService.getPublishedDetail(id));
    }

    @GetMapping("/admin/list")
    public Result<IPage<AnnouncementVO>> listAdminAnnouncements(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String status,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can view announcement management data");
        }
        return Result.success(announcementService.listAdminAnnouncements(page, size, status));
    }

    @GetMapping("/admin/{id}")
    public Result<AnnouncementVO> getAdminDetail(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can view announcement management data");
        }
        return Result.success(announcementService.getAdminDetail(id));
    }

    @PostMapping("/admin")
    public Result<Void> createAnnouncement(
            @RequestBody AnnouncementRequest request,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can create announcements");
        }
        announcementService.createAnnouncement(request, userId);
        return Result.success();
    }

    @PutMapping("/admin/{id}")
    public Result<Void> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can update announcements");
        }
        announcementService.updateAnnouncement(id, request);
        return Result.success();
    }

    @PostMapping("/admin/{id}/publish")
    public Result<Void> publishAnnouncement(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can publish announcements");
        }
        announcementService.publishAnnouncement(id);
        return Result.success();
    }

    @PostMapping("/admin/{id}/offline")
    public Result<Void> offlineAnnouncement(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can offline announcements");
        }
        announcementService.offlineAnnouncement(id);
        return Result.success();
    }

    @DeleteMapping("/admin/{id}")
    public Result<Void> deleteAnnouncement(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can delete announcements");
        }
        announcementService.deleteAnnouncement(id);
        return Result.success();
    }

    @PostMapping(value = "/admin/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<AnnouncementImageUploadVO> uploadAnnouncementImage(
            @RequestPart("file") MultipartFile file,
            @RequestHeader("X-User-Role") String role) {
        if (!"ADMIN".equals(role)) {
            return Result.forbidden("Only administrators can upload announcement images");
        }
        String imageKey = minioStorageService.uploadAnnouncementImage(file);
        return Result.success(new AnnouncementImageUploadVO(
                imageKey,
                minioStorageService.buildAnnouncementImageUrl(imageKey)
        ));
    }

    @GetMapping("/image")
    public ResponseEntity<InputStreamResource> getAnnouncementImage(@RequestParam String objectKey) {
        var stat = minioStorageService.statObject(objectKey);
        String contentType = StringUtils.hasText(stat.contentType())
                ? stat.contentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(stat.size())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .body(new InputStreamResource(minioStorageService.getObjectStream(objectKey)));
    }
}
