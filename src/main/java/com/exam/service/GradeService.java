package com.exam.service;

import com.exam.common.BizException;
import com.exam.dao.DetailDao;
import com.exam.dao.QuestionDao;
import com.exam.dao.SheetDao;
import com.exam.dao.Tx;
import com.exam.entity.AnswerDetail;
import com.exam.entity.AnswerSheet;
import com.exam.entity.Question;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GradeService {

    private final SheetDao sheetDao = new SheetDao();
    private final DetailDao detailDao = new DetailDao();
    private final QuestionDao questionDao = new QuestionDao();

    /** 待批改列表 */
    public List<Map<String, Object>> pending(long paperId) {
        return Tx.call(() -> sheetDao.listPendingGrade(paperId));
    }

    /** 某份答卷的主观题与作答内容，供教师逐题打分 */
    public List<Map<String, Object>> subjectiveItems(long sheetId) {
        return Tx.call(() -> {
            Map<Long, Question> questionMap = new LinkedHashMap<>();
            for (Question q : questionDao.listByPaper(sheetDao.findById(sheetId).getPaperId())) {
                questionMap.put(q.getId(), q);
            }
            List<Map<String, Object>> items = new java.util.ArrayList<>();
            for (AnswerDetail d : detailDao.listSubjective(sheetId)) {
                Question q = questionMap.get(d.getQuestionId());
                if (q == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", q.getId());
                item.put("content", q.getContent());
                item.put("reference", q.getAnswer());
                item.put("studentAnswer", d.getStudentAnswer());
                item.put("fullScore", q.getScore());
                items.add(item);
            }
            return items;
        });
    }

    /**
     * 批改：逐题给分（不超过满分），累加主观分后重算总分，状态置为已批改。
     * 客观分来自自动判分，这里不重复计算。
     */
    public Map<String, Object> grade(long sheetId, Map<String, Integer> questionScores) {
        return Tx.call(() -> {
            AnswerSheet s = sheetDao.findById(sheetId);
            if (s == null || s.getStatus() < 2) {
                throw new BizException("答卷不存在或尚未交卷");
            }
            if (s.getStatus() == 3) {
                throw new BizException("该答卷已批改");
            }

            int subjective = 0;
            for (AnswerDetail d : detailDao.listSubjective(sheetId)) {
                Question q = questionDao.findById(d.getQuestionId());
                Integer give = questionScores == null ? null : questionScores.get(String.valueOf(q.getId()));
                int score = give == null ? 0 : Math.max(0, Math.min(give, q.getScore()));
                // 主观题按得分率 60% 记为“掌握”，仅用于统计
                detailDao.updateSubjectiveScore(sheetId, q.getId(), score, score * 10 >= q.getScore() * 6 ? 1 : 0);
                subjective += score;
            }

            int total = s.getObjectiveScore() + subjective;
            sheetDao.updateScore(sheetId, s.getObjectiveScore(), subjective, total, 3);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sheetId", sheetId);
            m.put("subjectiveScore", subjective);
            m.put("totalScore", total);
            return m;
        });
    }
}
