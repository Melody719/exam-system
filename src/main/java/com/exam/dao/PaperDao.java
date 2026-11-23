package com.exam.dao;

import com.exam.entity.Paper;

import java.time.LocalDateTime;
import java.util.List;

public class PaperDao extends BaseDao<Paper> {

    public PaperDao() {
        super(Paper.class);
    }

    public long insert(Paper p) {
        return insert("INSERT INTO paper(course_id, title, mode, duration_minutes, start_time, end_time, "
                        + "total_score, status, create_by) VALUES(?,?,?,?,?,?,?,?,?)",
                p.getCourseId(), p.getTitle(), p.getMode(), p.getDurationMinutes(),
                p.getStartTime(), p.getEndTime(), p.getTotalScore(), p.getStatus(), p.getCreateBy());
    }

    public Paper findById(long id) {
        return queryOne("SELECT * FROM paper WHERE id = ?", id);
    }

    /** 发布是幂等的：只有草稿能改状态，重复点不会出错 */
    public int publish(long id) {
        return update("UPDATE paper SET status = 1 WHERE id = ? AND status = 0", id);
    }

    public int updateTotalScore(long id, int totalScore) {
        return update("UPDATE paper SET total_score = ? WHERE id = ?", totalScore, id);
    }

    /** 学生端：当前可考的试卷 */
    public List<Paper> listPublished(long courseId, LocalDateTime now) {
        return query("SELECT * FROM paper WHERE status = 1 AND course_id = ? AND end_time > ? ORDER BY start_time",
                courseId, now);
    }

    /** 教师端：该课程下我创建的所有试卷 */
    public List<Paper> listByCourse(long courseId) {
        return query("SELECT * FROM paper WHERE course_id = ? ORDER BY id DESC", courseId);
    }
}
