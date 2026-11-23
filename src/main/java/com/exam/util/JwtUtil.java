package com.exam.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 简易 JWT：Base64Url(payload) + "." + HMAC-SHA256 签名。
 * 不引第三方库，够用且原理可讲；生产可换 jjwt + 更长的密钥。
 */
public final class JwtUtil {

    private static final String SECRET = "exam-demo-secret-key";
    private static final long TTL_MS = 2 * 3600_000L;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtUtil() {
    }

    public static String create(long userId, String role) {
        long exp = System.currentTimeMillis() + TTL_MS;
        String payload = "{\"uid\":" + userId + ",\"role\":\"" + role + "\",\"exp\":" + exp + "}";
        String body = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(body);
    }

    /** 校验签名与过期时间，返回 payload；失败返回 null */
    public static JsonNode verify(String token) {
        try {
            if (token == null || !token.contains(".")) {
                return null;
            }
            String[] parts = token.split("\\.");
            if (parts.length != 2 || !sign(parts[0]).equals(parts[1])) {
                return null;
            }
            String json = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            JsonNode node = MAPPER.readTree(json);
            return node.get("exp").asLong() > System.currentTimeMillis() ? node : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String sign(String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("签名失败", e);
        }
    }
}
