package com.health.common.core;

public class BaseController {
    protected AjaxResult success() { return AjaxResult.success(); }
    protected AjaxResult success(Object data) { return AjaxResult.success(data); }
    protected AjaxResult success(String msg, Object data) { return AjaxResult.success(msg, data); }
    protected AjaxResult error() { return AjaxResult.error(); }
    protected AjaxResult error(String msg) { return AjaxResult.error(msg); }
    protected AjaxResult error(int code, String msg) { return AjaxResult.error(code, msg); }
    protected AjaxResult toPage(long total, java.util.List<?> rows) {
        AjaxResult r = PageResult.page(total, rows);
        if (total == 0) r.setEmptyHint(true);
        return r;
    }

    protected AjaxResult emptyData(String reason) {
        return AjaxResult.emptyData(reason);
    }
}
