package org.example.service;

import org.example.entity.ActivityProjection;
import org.example.entity.Announcement;
import org.example.entity.AnnouncementActivity;
import org.example.mapper.ActivityProjectionMapper;
import org.example.mapper.AnnouncementActivityMapper;
import org.example.mapper.AnnouncementAttachmentMapper;
import org.example.mapper.AnnouncementMapper;
import org.example.vo.AnnouncementVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceTest {

    @Mock
    private AnnouncementMapper announcementMapper;

    @Mock
    private AnnouncementActivityMapper announcementActivityMapper;

    @Mock
    private AnnouncementAttachmentMapper announcementAttachmentMapper;

    @Mock
    private ActivityProjectionMapper activityProjectionMapper;

    @Mock
    private MinioStorageService minioStorageService;

    private AnnouncementService announcementService;

    @BeforeEach
    void setUp() {
        announcementService = new AnnouncementService(
                announcementMapper,
                announcementActivityMapper,
                announcementAttachmentMapper,
                activityProjectionMapper,
                minioStorageService
        );
    }

    @Test
    void getPublishedDetailShouldReadActivityInfoFromProjection() {
        Announcement announcement = new Announcement();
        announcement.setId(1L);
        announcement.setTitle("Spring Volunteer Notice");
        announcement.setContent("Content");
        announcement.setStatus(AnnouncementService.STATUS_PUBLISHED);
        announcement.setPublishTime(LocalDateTime.of(2026, 4, 16, 10, 0));

        AnnouncementActivity link = new AnnouncementActivity();
        link.setAnnouncementId(1L);
        link.setActivityId(7L);

        ActivityProjection projection = new ActivityProjection();
        projection.setId(7L);
        projection.setTitle("Campus Cleanup");
        projection.setLocation("Playground");
        projection.setStatus("RECRUITING");
        projection.setCategory("ENV");
        projection.setStartTime(LocalDateTime.of(2026, 4, 20, 9, 0));
        projection.setEndTime(LocalDateTime.of(2026, 4, 20, 12, 0));

        when(announcementMapper.selectById(1L)).thenReturn(announcement);
        when(announcementActivityMapper.selectList(any())).thenReturn(List.of(link));
        when(activityProjectionMapper.selectBatchIds(any())).thenReturn(List.of(projection));
        when(announcementAttachmentMapper.selectList(any())).thenReturn(List.of());

        AnnouncementVO detail = announcementService.getPublishedDetail(1L);

        assertNotNull(detail);
        assertEquals(1, detail.getActivityIds().size());
        assertEquals(7L, detail.getActivityIds().get(0));
        assertEquals(1, detail.getActivities().size());
        assertEquals("Campus Cleanup", detail.getActivities().get(0).getTitle());
        assertEquals("Playground", detail.getActivities().get(0).getLocation());
        assertEquals("RECRUITING", detail.getActivities().get(0).getStatus());
    }
}
