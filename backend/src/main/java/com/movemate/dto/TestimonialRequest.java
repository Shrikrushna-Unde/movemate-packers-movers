package com.movemate.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A review submitted by a logged-in customer.
 *
 * <p>There is no {@code approved} field. If the client could send it, a customer could
 * self-approve their own review straight onto the homepage. Fields that only the server may
 * decide simply do not exist in the request DTO - the safest possible form of authorisation.
 */
public record TestimonialRequest(

        @NotBlank(message = "City is required")
        @Size(max = 80)
        String city,

        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be between 1 and 5")
        @Max(value = 5, message = "Rating must be between 1 and 5")
        Integer rating,

        @NotBlank(message = "Please write a short review")
        @Size(min = 20, max = 600, message = "Review must be between 20 and 600 characters")
        String message
) {
}
