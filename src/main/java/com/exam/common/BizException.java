package com.exam.common;

/** 业务异常：由全局异常处理器翻译成统一响应体，不打印堆栈 */
public class BizException extends RuntimeException {
    public BizException(String msg) {
        super(msg);
    }
}
