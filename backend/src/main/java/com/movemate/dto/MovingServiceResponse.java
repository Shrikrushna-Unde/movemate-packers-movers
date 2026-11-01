package com.movemate.dto;

import com.movemate.domain.MovingService;

import java.math.BigDecimal;

/**
 * A catalogue entry as the website receives it.
 */
public record MovingServiceResponse(
        Long id,
        String slug,
        String title,
        String shortDescription,
        String description,
        String icon,
        BigDecimal startingPrice,
        int displayOrder,
        boolean active
) {

    public static MovingServiceResponse from(MovingService s) {
        return new MovingServiceResponse(
                s.getId(),
                s.getSlug(),
                s.getTitle(),
                s.getShortDescription(),
                s.getDescription(),
                s.getIcon(),
                s.getStartingPrice(),
                s.getDisplayOrder(),
                s.isActive()
        );
    }
}
