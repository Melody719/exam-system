package com.exam.dao;

import com.exam.entity.OpLog;

public class LogDao extends BaseDao<OpLog> {

    public LogDao() {
        super(OpLog.class);
    }

    public long insert(Long userId, String action, String method, int costMs, int success) {
        return insert("INSERT INTO op_log(user_id, action, method, cost_ms, success) VALUES(?,?,?,?,?)",
                userId, action, method, costMs, success);
    }
}
