package com.exam.entity;

import lombok.Data;

@Data
public class QuestionOption {
    private Long id;
    private Long questionId;
    private String optKey;
    private String content;
}
