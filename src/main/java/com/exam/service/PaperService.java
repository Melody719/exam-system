package com.exam.service;

import com.exam.common.BizException;
import com.exam.dao.PaperDao;
import com.exam.dao.PaperQuestionDao;
import com.exam.dao.QuestionDao;
import com.exam.dao.Tx;
import com.exam.dto.PaperCreateReq;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PaperService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PaperDao paperDao = new PaperDao();
    private final PaperQuestionDao paperQuestionDao = new PaperQuestionDao();
    private final QuestionDao questionDao = new QuestionDao();

    /**
     * 组卷：固定卷直接用题目列表；随机卷按规则抽题。
     * 抽出来的题立刻写 paper_question —— 题目这刻起被“锁定”，
     * 同一场考试所有考生看到的是同一份，事后题库变动也不影响已发试卷。
     */
    public long create(PaperCreateReq req, long teacherId) {
        LocalDateTime start = LocalDateTime.parse(req.getStartTime(), FMT);
        LocalDateTime end = LocalDateTime.parse(req.getEndTime(), FMT);
        if (!end.isAfter(start)) {
            throw new BizException("结束时间必须晚于开始时间");
        }

        return Tx.call(() -> {
            List<Long> ids = "FIXED".equalsIgnoreCase(req.getMode())
                    ? new ArrayList<>(req.getQuestionIds())
                    : pickByRules(req);

            if (ids.isEmpty()) {
                throw new BizException("试卷没有题目");
            }

            int total = 0;
            for (Long qid : ids) {
                Question q = questionDao.findById(qid);
                if (q == null) {
                    throw new BizException("题目不存在: " + qid);
                }
                total += q.getScore();
            }

            Paper p = new Paper();
            p.setCourseId(req.getCourseId());
            p.setTitle(req.getTitle());
            p.setMode(req.getMode().toUpperCase());
            p.setDurationMinutes(req.getDurationMinutes());
            p.setStartTime(start);
            p.setEndTime(end);
            p.setTotalScore(total);
            p.setStatus(0);
            p.setCreateBy(teacherId);
            long paperId = paperDao.insert(p);

            int sort = 1;
            for (Long qid : ids) {
                paperQuestionDao.insert(paperId, qid, sort++);
            }
            return paperId;
        });
    }

    private List<Long> pickByRules(PaperCreateReq req) {
        List<Long> ids = new ArrayList<>();
        if (req.getRules() == null || req.getRules().isEmpty()) {
            throw new BizException("随机组卷必须提供抽题规则");
        }
        for (PaperCreateReq.Rule r : req.getRules()) {
            List<Question> picked = questionDao.randomPick(req.getCourseId(), r.getType(),
                    r.getDifficulty(), r.getKnowledgePoint(), r.getCount());
            if (picked.size() < r.getCount()) {
                throw new BizException("题库不足：题型 " + r.getType() + " 需要 "
                        + r.getCount() + " 题，仅抽到 " + picked.size() + " 题");
            }
            for (Question q : picked) {
                ids.add(q.getId());
            }
        }
        return ids;
    }

    public void publish(long paperId, long teacherId) {
        Tx.exec(() -> {
            Paper p = paperDao.findById(paperId);
            if (p == null || !p.getCreateBy().equals(teacherId)) {
                throw new BizException("试卷不存在");
            }
            if (paperDao.publish(paperId) == 0) {
                throw new BizException("试卷已发布，无需重复操作");
            }
        });
    }

    public List<Paper> listByCourse(long courseId) {
        return Tx.call(() -> paperDao.listByCourse(courseId));
    }
}
