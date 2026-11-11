package com.hospital.v2.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 统一响应封装（管理端 / 患者端共用）。 */
public record Result<T>(int code, String msg, T data) {
    public static final int OK = 200;
    public static final int FAIL = 500;
    public static final int BIZ_ERROR = 400;

    public static <T> Result<T> ok(T data) {
        return new Result<>(OK, "success", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(OK, "success", null);
    }

    public static <T> Result<T> fail(String msg) {
        return new Result<>(FAIL, msg, null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static Result<Map<String, Object>> page(long total, List<Map<String, Object>> rows) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", total);
        data.put("rows", rows);
        return new Result<>(OK, "success", data);
    }
}
