package com.bayerwestphalian.campaign.common.api;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last,
        boolean empty) {

    public PageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public static <T> PageResponse<T> of(
            List<T> content, int page, int size, long totalElements, int totalPages) {
        return new PageResponse<>(
                content,
                page,
                size,
                totalElements,
                totalPages,
                page <= 0,
                totalPages == 0 || page >= totalPages - 1,
                content == null || content.isEmpty());
    }

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty());
    }
}
