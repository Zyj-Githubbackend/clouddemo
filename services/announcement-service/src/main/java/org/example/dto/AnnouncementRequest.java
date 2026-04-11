package org.example.dto;

import lombok.Data;

import java.util.List;

@Data
public class AnnouncementRequest {

    private String title;

    private String content;

    private String imageKey;

    private List<String> imageKeys;

    private Long activityId;

    private String status;

    private Integer sortOrder;
}
