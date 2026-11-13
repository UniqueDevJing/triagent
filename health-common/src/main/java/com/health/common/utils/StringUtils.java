package com.health.common.utils;

import cn.hutool.core.util.StrUtil;

public class StringUtils {

    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static boolean isBlank(String str) {
        return StrUtil.isBlank(str);
    }

    public static boolean isNotBlank(String str) {
        return StrUtil.isNotBlank(str);
    }
}
