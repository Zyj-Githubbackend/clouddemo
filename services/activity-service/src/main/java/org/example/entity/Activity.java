package org.example.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("vol_activity")
public class Activity {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String description;
    
    private String location;
    
    private Integer maxParticipants;
    
    private Integer currentParticipants;
    
    private BigDecimal volunteerHours;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;

    /** 志愿招募开始时间（早于 {@link #registrationDeadline}） */
    private LocalDateTime registrationStartTime;
    
    private LocalDateTime registrationDeadline;
    
    private String status;
    
    private String category;
    
    private Long creatorId;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
