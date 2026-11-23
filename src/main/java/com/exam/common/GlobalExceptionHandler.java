package com.exam.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public Result<Void> biz(BizException e) {
        return Result.fail(e.getMessage());
    }

    /** 兜底：对外只给一句概话，细节进日志 */
    @ExceptionHandler(Exception.class)
    public Result<Void> other(Exception e) {
        log.error("未处理异常", e);
        return Result.fail("系统繁忙，请稍后重试");
    }
}
