package com.exam.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 数据源配置。项目不引连接池，直接 DriverManager；
 * Spring 启动时把 yml 里的配置灌进静态字段，DAO 层随时可取。
 */
@Component
public class DbConfig {

    private static String url;
    private static String username;
    private static String password;

    @Value("${spring.datasource.url}")
    public void setUrl(String v) {
        DbConfig.url = v;
    }

    @Value("${spring.datasource.username}")
    public void setUsername(String v) {
        DbConfig.username = v;
    }

    @Value("${spring.datasource.password}")
    public void setPassword(String v) {
        DbConfig.password = v;
    }

    static Connection open() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
