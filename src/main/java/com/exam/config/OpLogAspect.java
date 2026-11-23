package com.exam.config;

import com.exam.dao.LogDao;
import com.exam.dao.Tx;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 关键操作留痕。日志写库用独立事务，失败只打 warn，绝不影响主业务。
 */
@Slf4j
@Aspect
@Component
public class OpLogAspect {

    private final LogDao logDao = new LogDao();

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        Long uid = currentUid();
        String method = pjp.getSignature().toShortString();
        try {
            Object result = pjp.proceed();
            save(uid, opLog.value(), method, (int) (System.currentTimeMillis() - start), 1);
            return result;
        } catch (Throwable e) {
            save(uid, opLog.value(), method, (int) (System.currentTimeMillis() - start), 0);
            throw e;
        }
    }

    private void save(Long uid, String action, String method, int costMs, int success) {
        try {
            Tx.exec(() -> logDao.insert(uid, action, method, costMs, success));
        } catch (Exception e) {
            log.warn("操作日志写入失败: action={}", action, e);
        }
    }

    private Long currentUid() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        Object uid = attrs.getRequest().getAttribute("uid");
        return uid == null ? null : (Long) uid;
    }
}
