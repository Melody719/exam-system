-- 演示数据：1 个教师、1 个学生、1 门课、4 道客观题（25 分）
-- 密码均为 123456，salt/密文与 AuthService.hash 一致（salt+pwd+salt 的 SHA-256）
-- 本脚本幂等：先清空业务数据再灌演示数据，可以随时重跑
SET NAMES utf8mb4;

USE exam_db;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE answer_detail;
TRUNCATE TABLE answer_sheet;
TRUNCATE TABLE paper_question;
TRUNCATE TABLE paper;
TRUNCATE TABLE question_option;
TRUNCATE TABLE question;
TRUNCATE TABLE course;
TRUNCATE TABLE sys_user;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO sys_user(username, password, salt, real_name, role) VALUES
('teacher1', SHA2(CONCAT('aaaabbbbccccdddd', '123456', 'aaaabbbbccccdddd'), 256), 'aaaabbbbccccdddd', '张老师', 'TEACHER'),
('student1', SHA2(CONCAT('eeeefggghhhhiiii', '123456', 'eeeefggghhhhiiii'), 256), 'eeeefggghhhhiiii', '李同学', 'STUDENT');

INSERT INTO course(name, teacher_id) VALUES ('Java 程序设计', 1);

INSERT INTO question(course_id, type, difficulty, knowledge_point, content, answer, score) VALUES
(1, 1, 1, '基础语法', '以下哪个是 Java 的合法标识符？', 'B', 5),
(1, 1, 2, '集合', 'ArrayList 的底层数据结构是？', 'A', 5),
(1, 2, 2, '集合', '以下属于线程安全的集合有？', 'A,C', 10),
(1, 3, 1, '基础语法', 'Java 支持多继承。', 'B', 5);

INSERT INTO question_option(question_id, opt_key, content) VALUES
(1, 'A', '2abc'), (1, 'B', '_name'), (1, 'C', 'class'), (1, 'D', 'a-b'),
(2, 'A', '动态数组'), (2, 'B', '链表'), (2, 'C', '红黑树'), (2, 'D', '哈希表'),
(3, 'A', 'CopyOnWriteArrayList'), (3, 'B', 'ArrayList'), (3, 'C', 'ConcurrentHashMap'), (3, 'D', 'HashMap'),
(4, 'A', '正确'), (4, 'B', '错误');
