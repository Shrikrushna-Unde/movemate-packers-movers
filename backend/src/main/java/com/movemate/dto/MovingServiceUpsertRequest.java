package com.movemate.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Admin payload for creating or updating a catalogue entry.
 *
 * <p>"Upsert" because create and update take the same fields; the HTTP method and the presence
 * of an id in the path decide which one happens. Two identical DTOs would just drift apart.
 */
public record MovingServiceUpsertRequest(

        /*
         * A slug goes straight into a URL, so the character set is restricted:
         *   ^[a-z0-9]        must start with a letter or digit
         *   (-?[a-z0-9]+)*   then groups of characters optionally preceded by ONE hyphen
         *   $                and nothing else
         * This rejects "Home Shifting!", "--bad--" and trailing hyphens in one expression.
         */
        @NotBlank(message = "Slug is required")
        @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$",
                message = "Slug must be lowercase words separated by single hyphens")
        String slug,

        @NotBlank(message = "Title is required")
        @Size(max = 120)
        String title,

        @NotBlank(message = "Short description is required")
        @Size(max = 255)
        String shortDescription,

        @NotBlank(message = "Description is required")
        @Size(max = 1500)
        String description,

        @NotBlank(message = "Icon key is required")
        @Size(max = 40)
        String icon,

        @NotNull(message = "Starting price is required")
        @DecimalMin(value = "0", message = "Starting price cannot be negative")
        BigDecimal startingPrice,

        @Min(value = 0, message = "Display order cannot be negative")
        int displayOrder,

        boolean active
) {
}
