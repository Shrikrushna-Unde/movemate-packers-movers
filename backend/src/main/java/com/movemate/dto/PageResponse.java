package com.movemate.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * A stable, framework-free envelope for paginated results.
 *
 * <p>Why not return Spring's {@code Page<T>} straight from the controller? Because its JSON
 * shape is an implementation detail of Spring Data. It serialises a {@code Pageable} and a
 * {@code Sort} object, Spring Boot 3.3 logs a warning about exactly this, and the structure has
 * changed between versions - which would break the React app on a dependency upgrade. A small
 * DTO we own means our API contract is ours.
 *
 * @param content       the rows on this page
 * @param page          zero-based page number
 * @param size          requested page size
 * @param totalElements total matching rows across all pages
 * @param totalPages    total page count
 * @param first         convenience flags so the UI does not recompute them
 * @param last          ^
 * @param <T>           the response DTO type
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {

    /**
     * Converts a {@code Page<ENTITY>} into a {@code PageResponse<DTO>} in one call.
     *
     * <p>The {@code Function<E, T>} parameter is the per-row mapper, so callers write
     * {@code PageResponse.from(page, BookingResponse::from)}. Generic + method reference means
     * this single method serves bookings, enquiries and reviews with no duplication.
     */
    public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
