package com.exam.controller;

import com.exam.common.Result;
import com.exam.dao.CourseDao;
import com.exam.dao.Tx;
import com.exam.entity.Course;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 公共查询：登录后即可访问 */
@RestController
@RequestMapping("/api/common")
public class CommonController {

    private final CourseDao courseDao = new CourseDao();

    @GetMapping("/courses")
    public Result<List<Course>> courses() {
        return Result.ok(Tx.call(courseDao::listAll));
    }
}
