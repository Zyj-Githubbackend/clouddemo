package org.example.dto;

import lombok.Data;

@Data
public class AdminUserProfileUpdateRequest {

    private String username;

    private String realName;

    private String studentNo;

    private String phone;

    private String email;
}
