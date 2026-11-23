package com.exam.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Paper {
    private Long id;
    private Long courseId;
    private String title;
    private String mode;               // FIXED / RANDOM
    private Integer durationMinutes;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer totalScore;
    private Integer status;            // 0草稿 1已发布
    private Long createBy;
}
