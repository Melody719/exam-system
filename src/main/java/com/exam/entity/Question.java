package com.exam.entity;

import lombok.Data;

@Data
public class Question {
    private Long id;
    private Long courseId;
    private Integer type;          // 1单选 2多选 3判断 4主观
    private Integer difficulty;    // 1易 2中 3难
    private String knowledgePoint;
    private String content;
    private String answer;
    private Integer score;
}
