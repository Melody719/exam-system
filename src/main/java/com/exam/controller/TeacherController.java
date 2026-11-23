package com.exam.controller;

import com.exam.common.Result;
import com.exam.config.OpLog;
import com.exam.dto.PaperCreateReq;
import com.exam.dto.QuestionReq;
import com.exam.entity.Paper;
import com.exam.entity.Question;
import com.exam.service.GradeService;
import com.exam.service.PaperService;
import com.exam.service.QuestionService;
import com.exam.service.StatService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final QuestionService questionService;
    private final PaperService paperService;
    private final GradeService gradeService;
    private final StatService statService;

    public TeacherController(QuestionService questionService, PaperService paperService,
                             GradeService gradeService, StatService statService) {
        this.questionService = questionService;
        this.paperService = paperService;
        this.gradeService = gradeService;
        this.statService = statService;
    }

    // ---------- 题库 ----------

    @OpLog("新增题目")
    @PostMapping("/questions")
    public Result<Long> addQuestion(@RequestBody QuestionReq req) {
        return Result.ok(questionService.create(req));
    }

    @GetMapping("/questions")
    public Result<List<Question>> listQuestions(@RequestParam Long courseId) {
        return Result.ok(questionService.listByCourse(courseId));
    }

    // ---------- 组卷与发布 ----------

    @OpLog("组卷")
    @PostMapping("/papers")
    public Result<Long> createPaper(@RequestBody PaperCreateReq req,
                                    @RequestAttribute("uid") Long uid) {
        return Result.ok(paperService.create(req, uid));
    }

    @OpLog("发布考试")
    @PostMapping("/papers/{id}/publish")
    public Result<Void> publish(@PathVariable("id") Long id, @RequestAttribute("uid") Long uid) {
        paperService.publish(id, uid);
        return Result.ok();
    }

    @GetMapping("/papers/{id}")
    public Result<Paper> paper(@PathVariable("id") Long id) {
        return Result.ok(paperService.detail(id));
    }

    @GetMapping("/papers")
    public Result<List<Paper>> papers(@RequestParam Long courseId) {
        return Result.ok(paperService.listByCourse(courseId));
    }

    @GetMapping("/papers/{id}/sheets")
    public Result<List<Map<String, Object>>> sheets(@PathVariable("id") Long id) {
        return Result.ok(statService.sheetList(id));
    }

    // ---------- 批改 ----------

    @GetMapping("/papers/{id}/pending")
    public Result<List<Map<String, Object>>> pending(@PathVariable("id") Long id) {
        return Result.ok(gradeService.pending(id));
    }

    @GetMapping("/sheets/{sheetId}/subjective")
    public Result<List<Map<String, Object>>> subjective(@PathVariable Long sheetId) {
        return Result.ok(gradeService.subjectiveItems(sheetId));
    }

    /** body 形如 {"12": 8, "15": 6}：题目 id -> 得分 */
    @OpLog("批改")
    @PostMapping("/sheets/{sheetId}/grade")
    public Result<Map<String, Object>> grade(@PathVariable Long sheetId,
                                             @RequestBody Map<String, Integer> scores) {
        return Result.ok(gradeService.grade(sheetId, scores));
    }

    // ---------- 统计 ----------

    @GetMapping("/papers/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable("id") Long id) {
        return Result.ok(statService.overview(id));
    }

    @GetMapping("/papers/{id}/stats/question")
    public Result<List<Map<String, Object>>> questionStats(@PathVariable("id") Long id) {
        return Result.ok(statService.perQuestion(id));
    }

    @GetMapping("/papers/{id}/stats.csv")
    public void exportCsv(@PathVariable("id") Long id, HttpServletResponse resp) throws IOException {
        String csv = statService.csv(id);
        resp.setContentType("text/csv;charset=UTF-8");
        resp.setHeader("Content-Disposition", "attachment; filename=paper-" + id + ".csv");
        resp.getWriter().write(new String(csv.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
    }
}
