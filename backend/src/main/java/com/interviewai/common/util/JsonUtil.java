package com.interviewai.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.Map;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {
    }

    public static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize value to JSON", e);
        }
    }

    public static <T> List<T> readList(String json, Class<T> type) {
        try {
            if (json == null || json.isBlank()) {
                return List.of();
            }
            return MAPPER.readValue(json, MAPPER.getTypeFactory()
                    .constructCollectionType(List.class, type));
        } catch (Exception e) {
            return List.of();
        }
    }

    public static ArrayNode emptyArray() {
        return MAPPER.createArrayNode();
    }
}
