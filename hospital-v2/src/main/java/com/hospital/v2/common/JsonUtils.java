package com.hospital.v2.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Jackson 封装，避免到处 new ObjectMapper。 */
public final class JsonUtils {
    public static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }

    public static <T> T fromJson(String s, Class<T> type) {
        try {
            return MAPPER.readValue(s, type);
        } catch (Exception e) {
            return null;
        }
    }

    public static <T> T fromJson(String s, TypeReference<T> ref) {
        try {
            return MAPPER.readValue(s, ref);
        } catch (Exception e) {
            return null;
        }
    }
}
