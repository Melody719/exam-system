package com.exam.controller;

import com.exam.common.Result;
import com.exam.config.OpLog;
import com.exam.service.AuthService;
import com.exam.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody Map<String, String> body) {
        authService.register(body.get("username"), body.get("password"),
                body.get("realName"), body.get("role"));
        return Result.ok();
    }

    @OpLog("登录")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String token = authService.login(body.get("username"), body.get("password"));
        JsonNode payload = JwtUtil.verify(token);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("uid", payload.get("uid").asLong());
        data.put("role", payload.get("role").asText());
        return Result.ok(data);
    }
}
