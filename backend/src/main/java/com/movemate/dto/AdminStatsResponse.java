package com.movemate.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Numbers for the admin dashboard tiles.
 *
 * <p>{@code bookingsByStatus} is a {@code Map<String, Long>} rather than five named fields, so
 * adding a sixth {@link com.movemate.domain.BookingStatus} does not require touching this DTO,
 * the service and the React component. The key is the enum name, which the frontend already
 * knows how to label.
 */
public record AdminStatsResponse(
        long totalBookings,
        Map<String, Long> bookingsByStatus,
        /* Value of all non-cancelled bookings - the business pipeline, not realised revenue. */
        BigDecimal pipelineValue,
        long unhandledEnquiries,
        long pendingTestimonials,
        long totalCustomers,
        /* Null when there are no approved reviews yet, so the UI can hide the badge. */
        Double averageRating
) {
}
