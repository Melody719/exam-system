package com.exam.dao;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * 事务边界：连接存在 ThreadLocal 上，Service 层用 Tx.call 包一段业务，
 * 里面所有 DAO 自动共用同一条连接，成功 commit、异常 rollback。
 * 好处是“交卷 + 写明细 + 更新成绩”这类跨表操作天然是一个事务。
 */
public final class Tx {

    private static final ThreadLocal<Connection> HOLDER = new ThreadLocal<>();

    private Tx() {
    }

    public static Connection get() {
        Connection c = HOLDER.get();
        if (c == null) {
            throw new IllegalStateException("当前线程没有事务连接，请用 Tx.call/Tx.exec 包住业务代码");
        }
        return c;
    }

    @FunctionalInterface
    public interface Work<R> {
        R run() throws Exception;
    }

    @FunctionalInterface
    public interface Exec {
        void run() throws Exception;
    }

    public static <R> R call(Work<R> work) {
        // 已在事务里（Service 互调）：直接复用，不重复开事务
        if (HOLDER.get() != null) {
            try {
                return work.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        Connection conn = null;
        try {
            conn = DbConfig.open();
            conn.setAutoCommit(false);
            HOLDER.set(conn);

            R result = work.run();
            conn.commit();
            return result;
        } catch (RuntimeException | Error e) {
            rollback(conn);
            throw e;
        } catch (Exception e) {
            rollback(conn);
            throw new RuntimeException(e);
        } finally {
            HOLDER.remove();
            close(conn);
        }
    }

    public static void exec(Exec exec) {
        call(() -> {
            exec.run();
            return null;
        });
    }

    private static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {
            }
        }
    }

    private static void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignore) {
            }
        }
    }
}
