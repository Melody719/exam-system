package com.exam.dto;

import lombok.Data;

import java.util.List;

@Data
public class PaperCreateReq {
    private Long courseId;
    private String title;
    /** FIXED 固定卷 / RANDOM 随机卷 */
    private String mode;
    private Integer durationMinutes;
    private String startTime;   // yyyy-MM-dd HH:mm:ss
    private String endTime;
    /** 固定卷用 */
    private List<Long> questionIds;
    /** 随机卷用 */
    private List<Rule> rules;

    @Data
    public static class Rule {
        private Integer type;
        private Integer difficulty;
        private String knowledgePoint;   // 可空 = 不限
        private Integer count;
    }
}
