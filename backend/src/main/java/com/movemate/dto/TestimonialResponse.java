package com.movemate.dto;

import com.movemate.domain.Testimonial;

import java.time.Instant;

/**
 * A review as the website renders it.
 *
 * <p>No email, no user id - a public endpoint must expose the smallest useful projection.
 * Leaking a reviewer's email through a testimonial feed is a privacy incident, not a bug.
 */
public record TestimonialResponse(
        Long id,
        String authorName,
        String city,
        int rating,
        String message,
        boolean approved,
        Instant createdAt
) {

    public static TestimonialResponse from(Testimonial t) {
        return new TestimonialResponse(
                t.getId(),
                t.getAuthorName(),
                t.getCity(),
                t.getRating(),
                t.getMessage(),
                t.isApproved(),
                t.getCreatedAt()
        );
    }
}
