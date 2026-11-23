package com.exam.dao;

import com.exam.entity.PaperQuestion;

import java.util.List;

public class PaperQuestionDao extends BaseDao<PaperQuestion> {

    public PaperQuestionDao() {
        super(PaperQuestion.class);
    }

    public long insert(long paperId, long questionId, int sort) {
        return insert("INSERT INTO paper_question(paper_id, question_id, sort) VALUES(?,?,?)",
                paperId, questionId, sort);
    }

    public List<PaperQuestion> listByPaper(long paperId) {
        return query("SELECT * FROM paper_question WHERE paper_id = ? ORDER BY sort", paperId);
    }
}
