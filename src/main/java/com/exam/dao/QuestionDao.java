package com.exam.dao;

import com.exam.entity.Question;

import java.util.List;

public class QuestionDao extends BaseDao<Question> {

    public QuestionDao() {
        super(Question.class);
    }

    public long insert(Question q) {
        return insert("INSERT INTO question(course_id, type, difficulty, knowledge_point, content, answer, score) "
                        + "VALUES(?,?,?,?,?,?,?)",
                q.getCourseId(), q.getType(), q.getDifficulty(), q.getKnowledgePoint(),
                q.getContent(), q.getAnswer(), q.getScore());
    }

    public Question findById(long id) {
        return queryOne("SELECT * FROM question WHERE id = ?", id);
    }

    public List<Question> listByCourse(long courseId) {
        return query("SELECT * FROM question WHERE course_id = ? ORDER BY id DESC", courseId);
    }

    /** 按试卷取题（保持组卷时的顺序） */
    public List<Question> listByPaper(long paperId) {
        return query("SELECT q.* FROM question q "
                + "JOIN paper_question pq ON pq.question_id = q.id "
                + "WHERE pq.paper_id = ? ORDER BY pq.sort", paperId);
    }

    /**
     * 随机抽题：按题型/难度/知识点圈定范围后 ORDER BY RAND() 取 N 条。
     * difficulty 传 0 表示不限，knowledgePoint 传 null 表示不限。
     * 代价是临时表 + 全范围排序，题库量小时完全够用；顺序落库后即可复现。
     */
    public List<Question> randomPick(long courseId, int type, int difficulty, String knowledgePoint, int count) {
        String sql = "SELECT * FROM question WHERE course_id = ? AND type = ? AND (? = 0 OR difficulty = ?) "
                + "AND (? IS NULL OR knowledge_point = ?) ORDER BY RAND() LIMIT ?";
        return query(sql, courseId, type, difficulty, difficulty,
                isEmpty(knowledgePoint) ? null : knowledgePoint,
                isEmpty(knowledgePoint) ? null : knowledgePoint,
                count);
    }

    public List<Long> idsByCourse(long courseId) {
        return query("SELECT id FROM question WHERE course_id = ?", courseId)
                .stream().map(Question::getId).collect(java.util.stream.Collectors.toList());
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
