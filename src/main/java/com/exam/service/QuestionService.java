package com.exam.service;

import com.exam.common.BizException;
import com.exam.dao.OptionDao;
import com.exam.dao.QuestionDao;
import com.exam.dao.Tx;
import com.exam.dto.QuestionReq;
import com.exam.entity.Question;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QuestionService {

    private final QuestionDao questionDao = new QuestionDao();
    private final OptionDao optionDao = new OptionDao();

    /** 新增题目 + 选项，同一事务，避免出现没有选项的“半截题” */
    public long create(QuestionReq req) {
        if (req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new BizException("题干不能为空");
        }
        if (req.getScore() == null || req.getScore() <= 0) {
            throw new BizException("分值必须大于 0");
        }
        if (req.getType() != 4 && (req.getAnswer() == null || req.getAnswer().trim().isEmpty())) {
            throw new BizException("客观题必须设置答案");
        }

        return Tx.call(() -> {
            Question q = new Question();
            q.setCourseId(req.getCourseId());
            q.setType(req.getType());
            q.setDifficulty(req.getDifficulty());
            q.setKnowledgePoint(req.getKnowledgePoint() == null ? "未分类" : req.getKnowledgePoint());
            q.setContent(req.getContent());
            q.setAnswer(req.getAnswer() == null ? "" : req.getAnswer().toUpperCase());
            q.setScore(req.getScore());
            long id = questionDao.insert(q);

            List<QuestionReq.Opt> options = req.getOptions();
            if (options != null) {
                for (QuestionReq.Opt o : options) {
                    optionDao.insert(id, o.getOptKey(), o.getContent());
                }
            }
            return id;
        });
    }

    public List<Question> listByCourse(long courseId) {
        return Tx.call(() -> questionDao.listByCourse(courseId));
    }
}
