package com.exam.service;

import com.exam.dao.PaperDao;
import com.exam.dao.SheetDao;
import com.exam.dao.Tx;
import com.exam.entity.Paper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StatService {

    private final SheetDao sheetDao = new SheetDao();
    private final PaperDao paperDao = new PaperDao();

    /** 班级整体：平均分、及格率、最高最低 */
    public Map<String, Object> overview(long paperId) {
        return Tx.call(() -> {
            Paper p = paperDao.findById(paperId);
            int passLine = (int) Math.ceil(p.getTotalScore() * 0.6);

            List<Map<String, Object>> rows = sheetDao.overview(paperId, passLine);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("paperId", paperId);
            m.put("passLine", passLine);
            if (!rows.isEmpty()) {
                m.putAll(rows.get(0));
            }
            return m;
        });
    }

    /** 每题正确率：找出全班都错的题，作为讲评重点 */
    public List<Map<String, Object>> perQuestion(long paperId) {
        return Tx.call(() -> sheetDao.questionAccuracy(paperId));
    }

    public List<Map<String, Object>> sheetList(long paperId) {
        List<Map<String, Object>> list = new ArrayList<>();
        Tx.exec(() -> sheetDao.listByPaper(paperId).forEach(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("sheetId", s.getId());
            m.put("studentId", s.getStudentId());
            m.put("status", s.getStatus());
            m.put("submitTime", s.getSubmitTime());
            m.put("totalScore", s.getTotalScore());
            list.add(m);
        }));
        return list;
    }
}
