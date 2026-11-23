# 在线考试与题库练习系统

SpringBoot 2.7 + MySQL 8 + 自封装 JDBC DAO，无 ORM、无连接池。
带一个原生 JS 单页前端（`src/main/resources/static/index.html`），启动后浏览器直接用。

## 1. 技术栈

| 层   | 选型                                                    |
| --- | ----------------------------------------------------- |
| Web | Spring Boot 2.7.18 / Spring MVC                       |
| 持久层 | JDBC + 自封装 `BaseDao`（泛型 + 反射映射 + `PreparedStatement`） |
| 事务  | `ThreadLocal` 绑定 Connection，Service 层统一提交/回滚          |
| 鉴权  | 手写 HMAC-SHA256 JWT + 拦截器按角色做接口级授权                     |
| 数据库 | MySQL 8（8 张业务表 + 1 张操作日志表）                            |
| 其他  | AOP 操作日志、`@Scheduled` 定时收卷、CSV 导出                     |

## 2. 快速启动

```bash
# 1) 建库 + 演示数据
mysql -u root -p --default-character-set=utf8mb4 < sql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < sql/data.sql

# 2) 改连接配置
#    src/main/resources/application.yml 里的 username / password

# 3) 启动
mvn spring-boot:run

# 4) 打开页面
#    http://localhost:8080/
```

演示账号（密码都是 `123456`）：

- 教师：`teacher1`
- 学生：`student1`

## 2.1 页面怎么走一遍

前端是一个 HTML 文件 + 原生 `fetch`，没有 npm、没有构建步骤，直接由 Spring Boot 的
`static` 目录托管；`/api/**` 仍然全部走 JWT 拦截器，登录拿到的 token 存在 `localStorage`。

**教师侧**（用 `teacher1` 登录）：

1. 课程下拉选「Java 程序设计」；
2. 「组卷并发布」填标题、时长、考试时间窗，各题型填数量 → 随机组卷（题量不足会直接报错）；
3. 试卷列表里点「发布」，学生端立刻能看到这场考试；
4. 学生交卷后，点「统计」看平均分 / 及格率 / 每题正确率、导出 CSV；点「答卷」进待批改列表给主观题打分。

**学生侧**（用 `student1` 登录）：

1. 选课程 → 考试列表点「进入考试」；
2. 作答页顶部是服务端下发的剩余时间（前端倒计时只做展示，刷新不重置）；每题改动 0.8 秒后自动暂存；
3. 点「交卷」→ 客观题立刻出分，主观题显示「待批改」；或时间到自动交卷；
4. 「成绩」页看总分、每题对错和正确答案。

> 交卷后再点「进入考试」不会重开卷，只会跳到成绩页——这是 `(paper_id, student_id)` 唯一索引 + 状态机在兜底

## 3. 目构

```
src/main/java/com/exam
├── common/      Result、BizException、全局异常处理
├── config/      JwtInterceptor、WebConfig、@OpLog 注解与切面
├── dao/         DbConfig、Tx（事务）、BaseDao（泛型反射映射）、各表 DAO
├── dto/         QuestionReq、PaperCreateReq
├── entity/      8 张表对应实体
├── service/     Auth / Question / Paper / Exam / Grade / Stat
├── util/        JwtUtil
└── controller/  Auth / Common / Teacher / Student

src/main/resources
├── application.yml
└── static/index.html   单页前端（原生 JS，登录/组卷/答题/批改/统计）
```

## 4. 数据库设计（8 张业务表）

| 表                 | 作用                              | 关键索引                                                                    |
| ----------------- | ------------------------------- | ----------------------------------------------------------------------- |
| `sys_user`        | 用户，密码存 `SHA-256(salt+pwd+salt)` | `uk_username`                                                           |
| `course`          | 课程                              | -                                                                       |
| `question`        | 题库（题型/难度/知识点）                   | `idx_course_type(course_id,type,difficulty)`                            |
| `question_option` | 选项                              | `idx_question`                                                          |
| `paper`           | 试卷，同时承载考试时间窗与时长                 | -                                                                       |
| `paper_question`  | 试卷题目，组卷时落库锁题                    | `uk_paper_question`                                                     |
| `answer_sheet`    | 答卷（一份考试一个学生一条）                  | `uk_paper_student`、`idx_paper_status`、`idx_student_time`、`idx_deadline` |
| `answer_detail`   | 答题明细，兼做暂存区                      | `uk_sheet_question`                                                     |

> `op_log` 是 AOP 写入的操作日志表，不算业务表。

## 5. 关键设计

### 5.1 BaseDao：泛型 + 反射映射

- 一个 `BaseDao<T>` 承载 `query / queryOne / update / insert / queryMaps`，子类只写 SQL。
- 结果集映射按「列名下划线 → 字段驼峰」自动匹配，`Long/Integer/BigDecimal/LocalDateTime` 分别处理。
- 全部走 `PreparedStatement` 可变参数，天然防注入。
- `insert` 带 `RETURN_GENERATED_KEYS`，组卷/建卷时直接拿主键。

### 5.2 事务：ThreadLocal 连接 + 统一提交

```java
Tx.exec(() -> {
    sheetDao.markSubmitted(...);   // 改答卷状态
    detailDao.upsert(...);         // 写明细
    sheetDao.updateScore(...);     // 更新成绩
});                                // 成功 commit，异常 rollback
```

业务方法用 `Tx.call/Tx.exec` 包住即可，DAO 里从 `Tx.get()` 取同一条连接；嵌套调用复用外层事务，不会重复开。

### 5.3 组卷：一次成型、可复现

- 固定卷：直接按 `questionIds` 落 `paper_question`。
- 随机卷：按「题型 + 难度 + 知识点」逐条 `ORDER BY RAND() LIMIT n` 抽题，不足直接报错。
- 抽到就写库，题目从此锁定：同一场考试所有考生拿到同一份，题库后续增删不影响已发布试卷。
- `ORDER BY RAND()` 的代价是临时表 + 全范围排序，题库量小无感知；量大可换「随机主键区间 + 重试」。

### 5.4 限时交卷：服务端是唯一可信源

- `start` 时算 `deadline = min(进入时间 + 时长, 考试结束时间)` 并落库，前端只拿 `remainSeconds` 做展示。
- 暂存接口每次都校验 `status = 1 且 now <= deadline`，超时后连改答案都不允许。
- 交卷用条件更新抢占：`UPDATE ... SET status = 2 WHERE id = ? AND status = 1`，返回 0 即表示已被交过。
- 交卷时若 `now > deadline` 标记 `overtime = 1`，按已存答案照常判分。
- `@Scheduled(fixedDelay = 30s)` 扫 `status = 1 AND deadline < now` 的答卷兜底收卷，走同一个 `submitInternal`，因此和手动交卷互斥、不会重复判分。

### 5.5 自动判分

- 客观题：答案归一化（去空格/逗号、转大写、多选按字符排序）后比对，支持 `A,C` 与 `C,A` 等价。
- 主观题：明细记 `is_correct = -1` 进待批改队列，教师逐题给分（超过满分自动截断），重算 `总分 = 客观分 + 主观分`。
- 交卷时会把**未作答的题**也补一条明细，保证「每题正确率」的分母正确。

### 5.6 统计

- 概览：`COUNT / AVG / MAX / MIN` + `SUM(CASE WHEN total_score >= 及格线 ...)`，及格线取满分的 60%。
- 每题正确率：`answer_detail` 关联 `answer_sheet` 后 `GROUP BY question_id`。
- 导出 CSV 带 UTF-8 BOM，Excel 打开不乱码。

### 5.7 防重复与并发

- `(paper_id, student_id)` 唯一索引兜底「同一场考试两份成绩」；`start` 并发时捕获冲突再查一次，返回已有答卷。
- 交卷靠条件更新的影响行数判断谁抢到，抢不到的一方直接报「请勿重复提交」。
- 定时任务与手动交卷共用同一把「条件更新锁」，不存在双写。

## 6. 接口清单

### 认证

| 方法   | 路径                   | 说明                                |
| ---- | -------------------- | --------------------------------- |
| POST | `/api/auth/register` | `username/password/realName/role` |
| POST | `/api/auth/login`    | 返回 `token/uid/role`               |

请求头统一带 `Authorization: <token>`。

### 教师端

| 方法   | 路径                                         | 说明                 |
| ---- | ------------------------------------------ | ------------------ |
| POST | `/api/teacher/questions`                   | 新增题目（含选项）          |
| GET  | `/api/teacher/questions?courseId=`         | 题库列表               |
| POST | `/api/teacher/papers`                      | 组卷（FIXED / RANDOM） |
| POST | `/api/teacher/papers/{id}/publish`         | 发布考试               |
| GET  | `/api/teacher/papers/{id}`                 | 试卷详情               |
| GET  | `/api/teacher/papers?courseId=`            | 该课程的试卷列表（前端列表用）    |
| GET  | `/api/teacher/papers/{id}/sheets`          | 答卷列表               |
| GET  | `/api/teacher/papers/{id}/pending`         | 待批改列表              |
| GET  | `/api/teacher/sheets/{sheetId}/subjective` | 主观题作答              |
| POST | `/api/teacher/sheets/{sheetId}/grade`      | 批改，body `{"5":12}` |
| GET  | `/api/teacher/papers/{id}/stats`           | 班级概览               |
| GET  | `/api/teacher/papers/{id}/stats/question`  | 每题正确率              |
| GET  | `/api/teacher/papers/{id}/stats.csv`       | 导出 CSV             |

### 学生端

| 方法   | 路径                                     | 说明                                       |
| ---- | -------------------------------------- | ---------------------------------------- |
| GET  | `/api/student/papers?courseId=`        | 可参加考试                                    |
| POST | `/api/student/papers/{id}/start`       | 开始/继续（返回 sheetId、deadline、remainSeconds） |
| GET  | `/api/student/sheets/{sheetId}`        | 答题数据（不含答案）                               |
| POST | `/api/student/sheets/{sheetId}/save`   | 暂存 `{"3":"A","4":"B,C"}`                 |
| POST | `/api/student/sheets/{sheetId}/submit` | 交卷                                       |
| GET  | `/api/student/sheets/{sheetId}/result` | 成绩 + 错题回顾                                |
| GET  | `/api/student/sheets`                  | 我的考试记录                                   |

### 公共

| 方法  | 路径                    | 说明   |
| --- | --------------------- | ---- |
| GET | `/api/common/courses` | 课程列表 |

## 7. 响应体约定

```json
{ "code": 0, "msg": "ok", "data": {} }
```

`code = 0` 成功、`400` 业务异常（全局异常处理器翻译）、`401` 未登录、`403` 角色越权。


