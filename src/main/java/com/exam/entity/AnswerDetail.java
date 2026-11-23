package com.exam.entity;

import lombok.Data;

@Data
public class AnswerDetail {
    private Long id;
    private Long sheetId;
    private Long questionId;
    private String studentAnswer;
    private Integer isCorrect;     // 1对 0错 -1主观待批改
    private Integer score;
}
