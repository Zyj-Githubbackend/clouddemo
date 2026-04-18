package org.example.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class InternalUserSummary implements Serializable {

    private Long id;

    private String username;

    private String realName;

    private String studentNo;

    private String phone;
}
