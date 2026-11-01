package com.movemate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * "Contact us" form payload. This endpoint is fully public, so every field is validated
 * tightly - an unauthenticated write endpoint is the most exposed surface in the whole API.
 */
public record ContactRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 120)
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 180)
        String email,

        /*
         * Optional here, unlike on a booking - someone asking a general question should not be
         * forced to hand over a phone number. The regex still applies when a value IS given:
         * Bean Validation skips @Pattern for null (but not for an empty string, which is why
         * the frontend sends null rather than "" for an untouched optional field).
         */
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        @NotBlank(message = "Subject is required")
        @Size(min = 3, max = 150)
        String subject,

        @NotBlank(message = "Message is required")
        @Size(min = 10, max = 2000, message = "Message must be between 10 and 2000 characters")
        String message
) {
}
