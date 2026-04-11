package org.example.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class AnnouncementVO implements Serializable {

    private Long id;

    private String title;

    private String content;

    private String imageKey;

    private String imageUrl;

    private List<String> imageKeys;

    private List<String> imageUrls;

    private Long activityId;

    private String status;

    private Integer sortOrder;

    private Long publisherId;

    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
