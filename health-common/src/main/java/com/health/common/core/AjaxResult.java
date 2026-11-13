package com.health.common.core;

import lombok.Data;

@Data
public class AjaxResult {
    private int code;
    private String msg;
    private Object data;
    private Boolean emptyHint;
    private String emptyReason;

    private AjaxResult(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static AjaxResult success() {
        return new AjaxResult(200, "操作成功", null);
    }

    public static AjaxResult success(Object data) {
        return new AjaxResult(200, "操作成功", data);
    }

    public static AjaxResult success(String msg, Object data) {
        return new AjaxResult(200, msg, data);
    }

    public static AjaxResult emptyData(String reason) {
        AjaxResult r = new AjaxResult(200, "操作成功", null);
        r.emptyHint = true;
        r.emptyReason = reason;
        return r;
    }

    public AjaxResult withEmptyHint(String reason) {
        this.emptyHint = true;
        this.emptyReason = reason;
        return this;
    }

    public static AjaxResult error() {
        return new AjaxResult(500, "操作失败", null);
    }

    public static AjaxResult error(String msg) {
        return new AjaxResult(500, msg, null);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }
}