package com.exam.controller;

import com.exam.common.Result;
import com.exam.dto.PaperCreateReq;
import com.exam.dto.QuestionReq;
import com.exam.entity.Paper;
import com.exam.entity.Question;
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

import java.util.List;
import java.util.Map;

/** 教师端：题库维护、组卷发布、看统计。鉴权由 JwtInterceptor 按 /api/teacher 前缀统一拦截。 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final QuestionService questionService;
    private final PaperService paperService;
    private final StatService statService;

    public TeacherController(QuestionService questionService, PaperService paperService,
                             StatService statService) {
        this.questionService = questionService;
        this.paperService = paperService;
        this.statService = statService;
    }

    @PostMapping("/questions")
    public Result<Long> addQuestion(@RequestBody QuestionReq req) {
        return Result.ok(questionService.create(req));
    }

    @GetMapping("/questions")
    public Result<List<Question>> listQuestions(@RequestParam Long courseId) {
        return Result.ok(questionService.listByCourse(courseId));
    }

    @PostMapping("/papers")
    public Result<Long> createPaper(@RequestBody PaperCreateReq req,
                                    @RequestAttribute("uid") Long uid) {
        return Result.ok(paperService.create(req, uid));
    }

    @PostMapping("/papers/{id}/publish")
    public Result<Void> publish(@PathVariable("id") Long id,
                                @RequestAttribute("uid") Long uid) {
        paperService.publish(id, uid);
        return Result.ok();
    }

    @GetMapping("/papers")
    public Result<List<Paper>> papers(@RequestParam Long courseId) {
        return Result.ok(paperService.listByCourse(courseId));
    }

    @GetMapping("/papers/{id}/sheets")
    public Result<List<Map<String, Object>>> sheets(@PathVariable("id") Long id) {
        return Result.ok(statService.sheetList(id));
    }

    @GetMapping("/papers/{id}/stats")
    public Result<Map<String, Object>> stats(@PathVariable("id") Long id) {
        return Result.ok(statService.overview(id));
    }

    @GetMapping("/papers/{id}/stats/question")
    public Result<List<Map<String, Object>>> questionStats(@PathVariable("id") Long id) {
        return Result.ok(statService.perQuestion(id));
    }
}
