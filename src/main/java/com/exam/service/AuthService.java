package com.exam.service;

import com.exam.common.BizException;
import com.exam.dao.Tx;
import com.exam.dao.UserDao;
import com.exam.entity.User;
import com.exam.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

@Service
public class AuthService {

    private final UserDao userDao = new UserDao();

    public void register(String username, String password, String realName, String role) {
        if (username == null || username.trim().length() < 3) {
            throw new BizException("用户名至少 3 位");
        }
        if (!"STUDENT".equals(role) && !"TEACHER".equals(role)) {
            throw new BizException("角色不合法");
        }
        Tx.exec(() -> {
            if (userDao.findByUsername(username) != null) {
                throw new BizException("用户名已存在");
            }
            // 每个用户独立随机盐，同样的密码也存成不同密文
            String salt = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            userDao.insert(username, hash(password, salt), salt, realName, role);
        });
    }

    public String login(String username, String password) {
        return Tx.call(() -> {
            User u = userDao.findByUsername(username);
            if (u == null || !hash(password, u.getSalt()).equals(u.getPassword())) {
                throw new BizException("用户名或密码错误");
            }
            return JwtUtil.create(u.getId(), u.getRole());
        });
    }

    private static String hash(String pwd, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((salt + pwd + salt).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
