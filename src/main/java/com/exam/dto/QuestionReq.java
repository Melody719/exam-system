package com.exam.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionReq {
    private Long courseId;
    private Integer type;
    private Integer difficulty;
    private String knowledgePoint;
    private String content;
    private String answer;
    private Integer score;
    private List<Opt> options;

    @Data
    public static class Opt {
        private String optKey;
        private String content;
    }
}
