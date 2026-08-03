package com.interviewai.common.util;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public final class PageableUtils {

    private PageableUtils() {
    }

    public static Pageable build(int page, int size, String sortParam) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        if (sortParam == null || sortParam.isBlank()) {
            return PageRequest.of(safePage, safeSize);
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String clause : sortParam.split(",")) {
            String trimmed = clause.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.equalsIgnoreCase("asc") || trimmed.equalsIgnoreCase("desc")) {
                if (!orders.isEmpty()) {
                    Sort.Order prev = orders.remove(orders.size() - 1);
                    Sort.Direction dir = trimmed.equalsIgnoreCase("asc")
                            ? Sort.Direction.ASC : Sort.Direction.DESC;
                    orders.add(new Sort.Order(dir, prev.getProperty()));
                }
                continue;
            }
            String[] parts = trimmed.split(":");
            String property = parts[0];
            Sort.Direction direction = parts.length > 1 && parts[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            orders.add(new Sort.Order(direction, property));
        }
        return PageRequest.of(safePage, safeSize, Sort.by(orders));
    }
}
