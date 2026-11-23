package com.exam.dao;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 DAO：泛型 + 反射做结果集映射，全部 SQL 走 PreparedStatement（防注入）。
 * 约定：实体字段驼峰 <=> 表字段下划线，如 optKey <=> opt_key。
 */
public abstract class BaseDao<T> {

    private final Class<T> clazz;
    private final Map<String, Field> fieldMap = new HashMap<>();

    protected BaseDao(Class<T> clazz) {
        this.clazz = clazz;
        for (Field f : clazz.getDeclaredFields()) {
            f.setAccessible(true);
            fieldMap.put(toUnderline(f.getName()), f);
        }
    }

    /** 查列表 */
    protected List<T> query(String sql, Object... args) {
        try (PreparedStatement ps = bind(Tx.get().prepareStatement(sql), args);
             ResultSet rs = ps.executeQuery()) {
            List<T> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("查询失败: " + sql, e);
        }
    }

    protected T queryOne(String sql, Object... args) {
        List<T> list = query(sql, args);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 返回影响行数：交卷去重、条件更新都靠它判断是否“抢到” */
    protected int update(String sql, Object... args) {
        try (PreparedStatement ps = bind(Tx.get().prepareStatement(sql), args)) {
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("更新失败: " + sql, e);
        }
    }

    /** 插入并返回自增主键 */
    protected long insert(String sql, Object... args) {
        try (PreparedStatement ps = bind(
                Tx.get().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS), args)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw new RuntimeException("插入失败: " + sql, e);
        }
    }

    /** 统计用：直接拿 Map，省掉为聚合再建一个实体 */
    protected List<Map<String, Object>> queryMaps(String sql, Object... args) {
        try (PreparedStatement ps = bind(Tx.get().prepareStatement(sql), args);
             ResultSet rs = ps.executeQuery()) {
            ResultSetMetaData md = rs.getMetaData();
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                list.add(row);
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("查询失败: " + sql, e);
        }
    }

    private PreparedStatement bind(PreparedStatement ps, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) {
            Object v = args[i];
            if (v instanceof LocalDateTime) {
                ps.setTimestamp(i + 1, Timestamp.valueOf((LocalDateTime) v));
            } else {
                ps.setObject(i + 1, v);
            }
        }
        return ps;
    }

    private T mapRow(ResultSet rs) throws SQLException {
        try {
            T obj = clazz.getDeclaredConstructor().newInstance();
            ResultSetMetaData md = rs.getMetaData();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                Field f = fieldMap.get(md.getColumnLabel(i).toLowerCase());
                if (f == null) {
                    continue;
                }
                Class<?> type = f.getType();
                if (type == Long.class || type == long.class) {
                    f.set(obj, rs.getLong(i));
                } else if (type == Integer.class || type == int.class) {
                    f.set(obj, rs.getInt(i));
                } else if (type == BigDecimal.class) {
                    f.set(obj, rs.getBigDecimal(i));
                } else if (type == LocalDateTime.class) {
                    Timestamp ts = rs.getTimestamp(i);
                    f.set(obj, ts == null ? null : ts.toLocalDateTime());
                } else {
                    f.set(obj, rs.getObject(i));
                }
            }
            return obj;
        } catch (Exception e) {
            throw new RuntimeException("结果集映射失败: " + clazz.getSimpleName(), e);
        }
    }

    private static String toUnderline(String name) {
        StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append('_').append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
