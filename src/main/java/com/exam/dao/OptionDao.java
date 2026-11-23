package com.exam.dao;

import com.exam.entity.QuestionOption;

import java.util.List;

public class OptionDao extends BaseDao<QuestionOption> {

    public OptionDao() {
        super(QuestionOption.class);
    }

    public long insert(long questionId, String optKey, String content) {
        return insert("INSERT INTO question_option(question_id, opt_key, content) VALUES(?,?,?)",
                questionId, optKey, content);
    }

    public List<QuestionOption> listByQuestion(long questionId) {
        return query("SELECT * FROM question_option WHERE question_id = ? ORDER BY opt_key", questionId);
    }

    /** 批量取：答题页一次查完，避免 N+1 */
    public List<QuestionOption> listByQuestionIds(List<Long> ids) {
        if (ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        String marks = String.join(",", java.util.Collections.nCopies(ids.size(), "?"));
        return query("SELECT * FROM question_option WHERE question_id IN (" + marks + ") ORDER BY question_id, opt_key",
                ids.toArray());
    }
}
