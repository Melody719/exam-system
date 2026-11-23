package com.exam.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AnswerSheet {
    private Long id;
    private Long paperId;
    private Long studentId;
    private LocalDateTime startTime;
    private LocalDateTime deadline;
    private LocalDateTime submitTime;
    private Integer status;            // 1作答中 2已交卷 3已批改
    private Integer overtime;
    private Integer objectiveScore;
    private Integer subjectiveScore;
    private Integer totalScore;
}
