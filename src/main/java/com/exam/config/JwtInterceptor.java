package com.exam.config;

import com.exam.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * 统一鉴权：验 token 拿角色，再按 URL 前缀做接口级授权。
 * 学生手动改 URL 打教师接口 → 这里直接 403。
 */
public class JwtInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse resp, Object handler) throws Exception {
        String header = req.getHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : header;

        JsonNode payload = JwtUtil.verify(token);
        if (payload == null) {
            write(resp, 401, "未登录或登录已过期");
            return false;
        }

        String role = payload.get("role").asText();
        String path = req.getRequestURI();
        if (path.startsWith("/api/teacher/") && !"TEACHER".equals(role)) {
            write(resp, 403, "无权限访问教师接口");
            return false;
        }
        if (path.startsWith("/api/student/") && !"STUDENT".equals(role)) {
            write(resp, 403, "无权限访问学生接口");
            return false;
        }

        req.setAttribute("uid", payload.get("uid").asLong());
        req.setAttribute("role", role);
        return true;
    }

    private void write(HttpServletResponse resp, int code, String msg) throws Exception {
        resp.setStatus(code);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
    }
}
