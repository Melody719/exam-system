package com.exam.dao;

import com.exam.entity.Course;

import java.util.List;

public class CourseDao extends BaseDao<Course> {

    public CourseDao() {
        super(Course.class);
    }

    public long insert(String name, long teacherId) {
        return insert("INSERT INTO course(name, teacher_id) VALUES(?,?)", name, teacherId);
    }

    public Course findById(long id) {
        return queryOne("SELECT * FROM course WHERE id = ?", id);
    }

    public List<Course> listAll() {
        return query("SELECT * FROM course ORDER BY id");
    }
}
