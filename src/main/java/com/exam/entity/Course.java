package com.exam.entity;

import lombok.Data;

@Data
public class Course {
    private Long id;
    private String name;
    private Long teacherId;
}
