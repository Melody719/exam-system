package com.exam.dao;

import com.exam.entity.User;

public class UserDao extends BaseDao<User> {

    public UserDao() {
        super(User.class);
    }

    public User findByUsername(String username) {
        return queryOne("SELECT * FROM sys_user WHERE username = ?", username);
    }

    public long insert(String username, String password, String salt, String realName, String role) {
        return insert("INSERT INTO sys_user(username, password, salt, real_name, role) VALUES(?,?,?,?,?)",
                username, password, salt, realName, role);
    }
}
