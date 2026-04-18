package org.example.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("vol_announcement_activity_projection")
public class ActivityProjection {

    @TableId
    private Long id;

    private String title;

    private String location;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String status;

    private String category;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
