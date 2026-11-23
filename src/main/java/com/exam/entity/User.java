package com.exam.entity;

import lombok.Data;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String salt;
    private String realName;
    private String role;
}
