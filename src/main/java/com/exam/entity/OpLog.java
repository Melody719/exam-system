package com.exam.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OpLog {
    private Long id;
    private Long userId;
    private String action;
    private String method;
    private Integer costMs;
    private Integer success;
    private LocalDateTime createTime;
}
