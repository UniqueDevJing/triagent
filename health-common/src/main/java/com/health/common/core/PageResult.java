package com.health.common.core;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private List<T> rows;

    private PageResult(long total, List<T> rows) {
        this.total = total;
        this.rows = rows;
    }

    public static <T> AjaxResult page(long total, List<T> rows) {
        return AjaxResult.success(new PageResult<>(total, rows));
    }
}
