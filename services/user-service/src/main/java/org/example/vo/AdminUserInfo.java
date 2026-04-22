package org.example.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AdminUserInfo implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String studentNo;

    private String phone;

    private String email;

    private String role;

    private Integer status;

    private BigDecimal totalVolunteerHours;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
