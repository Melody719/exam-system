package com.exam.service;

import com.exam.common.BizException;
import com.exam.dao.DetailDao;
import com.exam.dao.OptionDao;
import com.exam.dao.PaperDao;
import com.exam.dao.QuestionDao;
import com.exam.dao.SheetDao;
import com.exam.dao.Tx;
import com.exam.entity.AnswerDetail;
import com.exam.entity.AnswerSheet;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.entity.QuestionOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作答主流程。核心原则：服务端是唯一可信源 —— 剩余时间、是否超时、能不能交卷，
 * 全部按库里 start_time / deadline 算，不采信前端倒计时。
 */
@Slf4j
@Service
public class ExamService {

    private final PaperDao paperDao = new PaperDao();
    private final SheetDao sheetDao = new SheetDao();
    private final DetailDao detailDao = new DetailDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final OptionDao optionDao = new OptionDao();

    /** 学生端：可参加的考试 */
    public List<Paper> listAvailablePapers(long courseId) {
        return Tx.call(() -> paperDao.listPublished(courseId, LocalDateTime.now()));
    }

    /**
     * 开始考试。重复进入返回原答卷（断点续考），不会重新计时；
     * 并发点两次由 (paper_id, student_id) 唯一索引兜底。
     */
    public Map<String, Object> start(long paperId, long studentId) {
        return Tx.call(() -> {
            Paper paper = paperDao.findById(paperId);
            if (paper == null || paper.getStatus() != 1) {
                throw new BizException("考试不存在或未发布");
            }
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(paper.getStartTime())) {
                throw new BizException("考试尚未开始");
            }
            if (now.isAfter(paper.getEndTime())) {
                throw new BizException("考试已结束");
            }

            AnswerSheet exist = sheetDao.findByPaperAndStudent(paperId, studentId);
            if (exist != null) {
                return sheetView(exist);
            }

            // 截止时间取 min(进入时间+时长, 考试结束时间)
            LocalDateTime deadline = now.plusMinutes(paper.getDurationMinutes());
            if (deadline.isAfter(paper.getEndTime())) {
                deadline = paper.getEndTime();
            }
            try {
                long sheetId = sheetDao.insert(paperId, studentId, now, deadline);
                return sheetView(sheetDao.findById(sheetId));
            } catch (RuntimeException e) {
                AnswerSheet again = sheetDao.findByPaperAndStudent(paperId, studentId);
                if (again != null) {
                    return sheetView(again);
                }
                throw e;
            }
        });
    }

    private Map<String, Object> sheetView(AnswerSheet s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sheetId", s.getId());
        m.put("paperId", s.getPaperId());
        m.put("status", s.getStatus());
        m.put("deadline", s.getDeadline());
        m.put("remainSeconds", Math.max(Duration.between(LocalDateTime.now(), s.getDeadline()).getSeconds(), 0));
        return m;
    }

    /** 答题页数据：题目 + 选项 + 已暂存答案（不含正确答案） */
    public Map<String, Object> loadSheet(long sheetId, long studentId) {
        return Tx.call(() -> {
            AnswerSheet s = requireSheet(sheetId, studentId);
            List<Question> questions = questionDao.listByPaper(s.getPaperId());
            List<Long> qids = questions.stream().map(Question::getId).collect(Collectors.toList());
            Map<Long, List<QuestionOption>> optMap = optionDao.listByQuestionIds(qids).stream()
                    .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
            Map<Long, String> answered = new HashMap<>();
            for (AnswerDetail d : detailDao.listBySheet(sheetId)) {
                if (d.getStudentAnswer() != null) {
                    answered.put(d.getQuestionId(), d.getStudentAnswer());
                }
            }

            List<Map<String, Object>> items = new ArrayList<>();
            for (Question q : questions) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", q.getId());
                item.put("type", q.getType());
                item.put("content", q.getContent());
                item.put("score", q.getScore());
                item.put("options", optMap.getOrDefault(q.getId(), new ArrayList<>()));
                item.put("myAnswer", answered.get(q.getId()));
                items.add(item);
            }

            Map<String, Object> m = sheetView(s);
            m.put("questions", items);
            return m;
        });
    }

    /** 暂存：允许在截止前反复覆盖，逐题 upsert */
    public void save(long sheetId, long studentId, Map<String, String> answers) {
        Tx.exec(() -> {
            AnswerSheet s = requireSheet(sheetId, studentId);
            if (s.getStatus() != 1) {
                throw new BizException("已交卷，无法修改答案");
            }
            if (LocalDateTime.now().isAfter(s.getDeadline())) {
                throw new BizException("已超时，无法修改答案");
            }
            if (answers == null) {
                return;
            }
            for (Map.Entry<String, String> e : answers.entrySet()) {
                detailDao.upsert(sheetId, Long.parseLong(e.getKey()), e.getValue(), null, 0);
            }
        });
    }

    /** 学生主动交卷 */
    public Map<String, Object> submit(long sheetId, long studentId) {
        return Tx.call(() -> {
            AnswerSheet s = requireSheet(sheetId, studentId);
            if (!submitInternal(s)) {
                throw new BizException("已交卷，请勿重复提交");
            }
            AnswerSheet fresh = sheetDao.findById(sheetId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sheetId", fresh.getId());
            m.put("status", fresh.getStatus());
            m.put("overtime", fresh.getOvertime() == 1);
            m.put("totalScore", fresh.getTotalScore());
            return m;
        });
    }

    /**
     * 收卷的唯一入口：条件更新 status=1 -> 2 抢占，抢到才判分。
     * 学生点交卷和定时任务同时到达时，只有一方能成功，不会重复判分/重复写。
     */
    private boolean submitInternal(AnswerSheet s) {
        LocalDateTime now = LocalDateTime.now();
        int overtime = now.isAfter(s.getDeadline()) ? 1 : 0;
        if (sheetDao.markSubmitted(s.getId(), now, overtime) == 0) {
            return false;
        }
        sheetDao.finish(s.getId(), autoGrade(s.getId(), s.getPaperId()));
        return true;
    }

    /** 客观题自动判分，并把每一题的结果补写进明细（含未作答的题） */
    private int autoGrade(long sheetId, long paperId) {
        Map<Long, String> answered = new HashMap<>();
        for (AnswerDetail d : detailDao.listBySheet(sheetId)) {
            answered.put(d.getQuestionId(), d.getStudentAnswer() == null ? "" : d.getStudentAnswer());
        }

        int total = 0;
        for (Question q : questionDao.listByPaper(paperId)) {
            String ans = answered.getOrDefault(q.getId(), "");
            boolean right = !ans.isEmpty() && normalize(q.getAnswer()).equals(normalize(ans));
            int score = right ? q.getScore() : 0;
            total += score;
            detailDao.upsert(sheetId, q.getId(), ans, right ? 1 : 0, score);
        }
        return total;
    }

    /** 兜底收卷：每 30 秒扫一次过期未交的答卷 */
    @Scheduled(fixedDelay = 30_000)
    public void autoSubmitExpired() {
        List<Long> ids = Tx.call(() -> sheetDao.listExpiredIds(LocalDateTime.now()));
        for (Long id : ids) {
            try {
                Tx.exec(() -> {
                    AnswerSheet s = sheetDao.findById(id);
                    if (s != null && s.getStatus() == 1 && submitInternal(s)) {
                        log.info("定时收卷: sheetId={}", id);
                    }
                });
            } catch (Exception e) {
                log.warn("定时收卷失败: sheetId={}", id, e);
            }
        }
    }

    /** 成绩与错题回顾（交卷后可见） */
    public Map<String, Object> result(long sheetId, long studentId) {
        return Tx.call(() -> {
            AnswerSheet s = requireSheet(sheetId, studentId);
            if (s.getStatus() < 2) {
                throw new BizException("尚未交卷，暂无成绩");
            }
            Map<Long, AnswerDetail> detailMap = detailDao.listBySheet(sheetId).stream()
                    .collect(Collectors.toMap(AnswerDetail::getQuestionId, d -> d, (a, b) -> a));

            List<Map<String, Object>> items = new ArrayList<>();
            for (Question q : questionDao.listByPaper(s.getPaperId())) {
                AnswerDetail d = detailMap.get(q.getId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", q.getId());
                item.put("content", q.getContent());
                item.put("myAnswer", d == null ? "" : d.getStudentAnswer());
                item.put("correctAnswer", q.getAnswer());
                item.put("isCorrect", d == null ? 0 : d.getIsCorrect());
                item.put("score", d == null ? 0 : d.getScore());
                item.put("fullScore", q.getScore());
                items.add(item);
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sheetId", sheetId);
            m.put("status", s.getStatus());
            m.put("overtime", s.getOvertime() == 1);
            m.put("totalScore", s.getTotalScore());
            m.put("details", items);
            return m;
        });
    }

    public List<AnswerSheet> mySheets(long studentId) {
        return Tx.call(() -> sheetDao.listByStudent(studentId));
    }

    /** 水平越权防线：答卷必须属于当前登录学生 */
    private AnswerSheet requireSheet(long sheetId, long studentId) {
        AnswerSheet s = sheetDao.findById(sheetId);
        if (s == null || !s.getStudentId().equals(studentId)) {
            throw new BizException("答卷不存在");
        }
        return s;
    }

    /** 答案归一化：去空格与分隔符、转大写、多选按字符排序（A,C 与 C,A 等价） */
    static String normalize(String answer) {
        if (answer == null) {
            return "";
        }
        String t = answer.replaceAll("[\\s,，、]", "").toUpperCase();
        char[] cs = t.toCharArray();
        Arrays.sort(cs);
        return new String(cs);
    }
}
