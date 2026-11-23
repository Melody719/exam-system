package com.exam.dao;

import com.exam.entity.AnswerSheet;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class SheetDao extends BaseDao<AnswerSheet> {

    public SheetDao() {
        super(AnswerSheet.class);
    }

    public long insert(long paperId, long studentId, LocalDateTime start, LocalDateTime deadline) {
        return insert("INSERT INTO answer_sheet(paper_id, student_id, start_time, deadline, status) "
                        + "VALUES(?,?,?,?,1)",
                paperId, studentId, start, deadline);
    }

    public AnswerSheet findById(long id) {
        return queryOne("SELECT * FROM answer_sheet WHERE id = ?", id);
    }

    public AnswerSheet findByPaperAndStudent(long paperId, long studentId) {
        return queryOne("SELECT * FROM answer_sheet WHERE paper_id = ? AND student_id = ?", paperId, studentId);
    }

    /**
     * 交卷的原子闸门：只有 status=1 的那一次能改成功。
     * 返回 0 说明已经被别人（或定时任务）交过了，调用方应放弃后续写操作。
     */
    public int markSubmitted(long sheetId, LocalDateTime submitTime, int overtime) {
        return update("UPDATE answer_sheet SET status = 2, submit_time = ?, overtime = ? "
                + "WHERE id = ? AND status = 1", submitTime, overtime, sheetId);
    }

    public int updateScore(long sheetId, int objective, int subjective, int total, int status) {
        return update("UPDATE answer_sheet SET objective_score = ?, subjective_score = ?, "
                + "total_score = ?, status = ? WHERE id = ?", objective, subjective, total, status, sheetId);
    }

    /** 定时收卷扫描：到点还没交的 */
    public List<Long> listExpiredIds(LocalDateTime now) {
        return query("SELECT * FROM answer_sheet WHERE status = 1 AND deadline < ? LIMIT 200", now)
                .stream().map(AnswerSheet::getId).collect(java.util.stream.Collectors.toList());
    }

    public List<AnswerSheet> listByPaper(long paperId) {
        return query("SELECT * FROM answer_sheet WHERE paper_id = ? ORDER BY total_score DESC", paperId);
    }

    public List<AnswerSheet> listByStudent(long studentId) {
        return query("SELECT * FROM answer_sheet WHERE student_id = ? ORDER BY start_time DESC", studentId);
    }

    /** 待批改：有主观题且已交卷 */
    public List<Map<String, Object>> listPendingGrade(long paperId) {
        return queryMaps("SELECT s.id sheet_id, s.student_id, u.real_name, s.submit_time "
                + "FROM answer_sheet s JOIN sys_user u ON u.id = s.student_id "
                + "WHERE s.paper_id = ? AND s.status = 2 ORDER BY s.submit_time", paperId);
    }

    /** 班级概览：一场考试只算已交卷/已批改的答卷 */
    public List<Map<String, Object>> overview(long paperId, int passLine) {
        return queryMaps("SELECT COUNT(*) sheet_count, ROUND(IFNULL(AVG(total_score),0), 1) avg_score, "
                + "IFNULL(MAX(total_score),0) max_score, IFNULL(MIN(total_score),0) min_score, "
                + "SUM(CASE WHEN total_score >= ? THEN 1 ELSE 0 END) pass_count "
                + "FROM answer_sheet WHERE paper_id = ? AND status >= 2", passLine, paperId);
    }

    /** 每题正确率 */
    public List<Map<String, Object>> questionAccuracy(long paperId) {
        return queryMaps("SELECT q.content, COUNT(d.id) total, "
                + "SUM(CASE WHEN d.is_correct = 1 THEN 1 ELSE 0 END) correct, "
                + "ROUND(SUM(CASE WHEN d.is_correct = 1 THEN 1 ELSE 0 END) * 100 / COUNT(d.id), 1) rate "
                + "FROM answer_detail d "
                + "JOIN answer_sheet s ON s.id = d.sheet_id "
                + "JOIN question q ON q.id = d.question_id "
                + "WHERE s.paper_id = ? AND s.status >= 2 GROUP BY q.id, q.content ORDER BY q.id", paperId);
    }

    /** 学生成绩明细，用于 CSV 导出 */
    public List<Map<String, Object>> studentScores(long paperId) {
        return queryMaps("SELECT s.student_id, u.real_name, s.total_score, s.objective_score, "
                + "s.subjective_score, s.overtime FROM answer_sheet s "
                + "JOIN sys_user u ON u.id = s.student_id "
                + "WHERE s.paper_id = ? AND s.status >= 2 ORDER BY s.total_score DESC", paperId);
    }
}
