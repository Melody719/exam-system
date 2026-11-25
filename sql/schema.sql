-- 在线考试系统 · 8 张表
-- 文件是 UTF-8 编码，Windows 的 mysql 客户端默认字符集常是 gbk，这里显式声明一次
SET NAMES utf8mb4;

DROP DATABASE IF EXISTS exam_db;
CREATE DATABASE exam_db DEFAULT CHARSET utf8mb4;
USE exam_db;

-- 1. 用户
CREATE TABLE sys_user (
    id        BIGINT PRIMARY KEY AUTO_INCREMENT,
    username  VARCHAR(50)  NOT NULL,
    password  CHAR(64)     NOT NULL COMMENT 'SHA-256(salt+pwd+salt)',
    salt      CHAR(16)     NOT NULL,
    real_name VARCHAR(50)  NOT NULL,
    role      VARCHAR(10)  NOT NULL COMMENT 'STUDENT / TEACHER',
    UNIQUE KEY uk_username (username)
);

-- 2. 课程
CREATE TABLE course (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    name       VARCHAR(100) NOT NULL,
    teacher_id BIGINT       NOT NULL
);

-- 3. 题库
CREATE TABLE question (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id       BIGINT       NOT NULL,
    type            TINYINT      NOT NULL COMMENT '1单选 2多选 3判断',
    difficulty      TINYINT      NOT NULL COMMENT '1易 2中 3难',
    knowledge_point VARCHAR(50)  NOT NULL,
    content         VARCHAR(500) NOT NULL,
    answer          VARCHAR(200) NOT NULL COMMENT '选项键，如 A 或 A,C',
    score           INT          NOT NULL,
    KEY idx_course_type (course_id, type, difficulty)
);

-- 4. 选项
CREATE TABLE question_option (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    question_id BIGINT      NOT NULL,
    opt_key     CHAR(1)     NOT NULL,
    content     VARCHAR(300) NOT NULL,
    KEY idx_question (question_id)
);

-- 5. 试卷（同时承载“考试”信息：发布 + 时间窗 + 时长）
CREATE TABLE paper (
    id               BIGINT PRIMARY KEY AUTO_INCREMENT,
    course_id        BIGINT       NOT NULL,
    title            VARCHAR(100) NOT NULL,
    mode             VARCHAR(10)  NOT NULL COMMENT 'FIXED / RANDOM',
    duration_minutes INT          NOT NULL,
    start_time       DATETIME     NOT NULL,
    end_time         DATETIME     NOT NULL,
    total_score      INT          NOT NULL DEFAULT 0,
    status           TINYINT      NOT NULL DEFAULT 0 COMMENT '0草稿 1已发布',
    create_by        BIGINT       NOT NULL
);

-- 6. 试卷题目（组卷那一刻落库，题目被锁定，全场考生一致）
CREATE TABLE paper_question (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id    BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    sort        INT    NOT NULL,
    UNIQUE KEY uk_paper_question (paper_id, question_id)
);

-- 7. 答卷
CREATE TABLE answer_sheet (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    paper_id    BIGINT   NOT NULL,
    student_id  BIGINT   NOT NULL,
    start_time  DATETIME NOT NULL,
    deadline    DATETIME NOT NULL COMMENT '服务端计算的截止时间',
    submit_time DATETIME NULL,
    status      TINYINT  NOT NULL DEFAULT 1 COMMENT '1作答中 2已交卷',
    overtime    TINYINT  NOT NULL DEFAULT 0 COMMENT '是否超时交卷',
    total_score INT      NOT NULL DEFAULT 0,
    -- 同一场考试一个学生只能有一份成绩
    UNIQUE KEY uk_paper_student (paper_id, student_id),
    KEY idx_paper_status (paper_id, status),
    KEY idx_student_time (student_id, start_time),
    KEY idx_deadline (status, deadline)
);

-- 8. 答题明细（兼做暂存区）
CREATE TABLE answer_detail (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    sheet_id       BIGINT       NOT NULL,
    question_id    BIGINT       NOT NULL,
    student_answer VARCHAR(200) NULL,
    is_correct     TINYINT      NULL COMMENT '1对 0错 NULL未作答',
    score          INT          NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sheet_question (sheet_id, question_id)
);
