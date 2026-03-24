package org.example.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RegistrationVO implements Serializable {
    
    private Long id;

    /** 报名用户 ID（管理员列表接口返回） */
    private Long userId;
    
    private Long activityId;
    
    private String activityTitle;

    /** 以下字段由管理员报名列表 SQL 联表 sys_user 填充 */
    private String username;
    private String realName;
    private String studentNo;
    private String phone;
    
    private String location;
    
    private BigDecimal volunteerHours;
    
    private LocalDateTime startTime;
    
    private LocalDateTime registrationTime;
    
    private Integer checkInStatus;
    
    private Integer hoursConfirmed;
    
    private String status;
}
