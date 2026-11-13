package com.health.common.core;

import lombok.Data;
import java.util.List;

@Data
public class PageResult<T> {
    private long total;
    private List<T> records;

    private PageResult(long total, List<T> records) {
        this.total = total;
        this.records = records;
    }

    public static <T> AjaxResult page(long total, List<T> rows) {
        return AjaxResult.success(new PageResult<>(total, rows));
    }
}
