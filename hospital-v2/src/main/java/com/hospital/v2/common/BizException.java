package com.hospital.v2.common;

/** 业务异常（用于二次核验失败、号源不足等可控错误）。 */
public class BizException extends RuntimeException {
    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
