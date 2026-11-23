package com.exam.controller;

import com.exam.common.Result;
import com.exam.config.OpLog;
import com.exam.entity.AnswerSheet;
import com.exam.entity.Paper;
import com.exam.service.ExamService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController {

    private final ExamService examService;

    public StudentController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping("/papers")
    public Result<List<Paper>> papers(@RequestParam Long courseId) {
        return Result.ok(examService.listAvailablePapers(courseId));
    }

    /** 开始/继续考试 */
    @OpLog("进入考试")
    @PostMapping("/papers/{id}/start")
    public Result<Map<String, Object>> start(@PathVariable("id") Long id,
                                             @RequestAttribute("uid") Long uid) {
        return Result.ok(examService.start(id, uid));
    }

    /** 答题页数据（无正确答案） */
    @GetMapping("/sheets/{sheetId}")
    public Result<Map<String, Object>> sheet(@PathVariable Long sheetId,
                                             @RequestAttribute("uid") Long uid) {
        return Result.ok(examService.loadSheet(sheetId, uid));
    }

    /** 暂存，body 形如 {"3":"A","4":"B,C"}：题目 id -> 答案 */
    @PostMapping("/sheets/{sheetId}/save")
    public Result<Void> save(@PathVariable Long sheetId,
                             @RequestAttribute("uid") Long uid,
                             @RequestBody Map<String, String> answers) {
        examService.save(sheetId, uid, answers);
        return Result.ok();
    }

    @OpLog("交卷")
    @PostMapping("/sheets/{sheetId}/submit")
    public Result<Map<String, Object>> submit(@PathVariable Long sheetId,
                                              @RequestAttribute("uid") Long uid) {
        return Result.ok(examService.submit(sheetId, uid));
    }

    @GetMapping("/sheets/{sheetId}/result")
    public Result<Map<String, Object>> result(@PathVariable Long sheetId,
                                              @RequestAttribute("uid") Long uid) {
        return Result.ok(examService.result(sheetId, uid));
    }

    @GetMapping("/sheets")
    public Result<List<AnswerSheet>> mySheets(@RequestAttribute("uid") Long uid) {
        return Result.ok(examService.mySheets(uid));
    }
}
