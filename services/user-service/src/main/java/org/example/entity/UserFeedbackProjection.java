package org.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_feedback_projection")
public class UserFeedbackProjection {

    @TableId(value = "feedback_id", type = IdType.INPUT)
    private Long feedbackId;

    private Long userId;

    private String title;

    private String category;

    private String status;

    private String sourceMessageId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
