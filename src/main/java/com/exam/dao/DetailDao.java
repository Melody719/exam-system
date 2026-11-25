package com.exam.dao;

import com.exam.entity.AnswerDetail;

import java.util.List;

public class DetailDao extends BaseDao<AnswerDetail> {

    public DetailDao() {
        super(AnswerDetail.class);
    }

    /**
     * 暂存/判分共用一条 upsert：唯一键 (sheet_id, question_id) 保证一题一行。
     * 暂存时 isCorrect 传 null，判分时再补。
     */
    public void upsert(long sheetId, long questionId, String answer, Integer isCorrect, int score) {
        update("INSERT INTO answer_detail(sheet_id, question_id, student_answer, is_correct, score) "
                        + "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE student_answer = VALUES(student_answer), "
                        + "is_correct = VALUES(is_correct), score = VALUES(score)",
                sheetId, questionId, answer, isCorrect, score);
    }

    public List<AnswerDetail> listBySheet(long sheetId) {
        return query("SELECT * FROM answer_detail WHERE sheet_id = ?", sheetId);
    }
}
