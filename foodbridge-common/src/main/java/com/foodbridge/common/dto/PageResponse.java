package com.foodbridge.common.dto;

import java.util.List;

/**
 * Lightweight, framework-agnostic pagination wrapper returned by list endpoints.
 *
 * @param content       items in the current page
 * @param page          zero-based page index
 * @param size          requested page size
 * @param totalElements total number of matching elements across all pages
 * @param totalPages    total number of pages
 * @param <T>           element type
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }
}
