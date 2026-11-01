package com.movemate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Signup payload.
 *
 * <p>Why a {@code record} and not a class? A record is an immutable carrier of data: the
 * compiler generates the constructor, accessors, {@code equals}, {@code hashCode} and
 * {@code toString}. A DTO has no behaviour and no identity, so immutability is exactly right -
 * and it removes about 60 lines of boilerplate per DTO. Records arrived in Java 16, so this
 * also signals that you write modern Java.
 *
 * <p>Why a DTO at all, instead of accepting the {@code User} entity directly?
 * <ul>
 *   <li><b>Security.</b> If the endpoint bound to {@code User}, a caller could POST
 *       {@code {"role":"ROLE_ADMIN"}} and promote themselves. That is mass assignment, and it
 *       is a real vulnerability, not a style preference.</li>
 *   <li><b>Contract stability.</b> Renaming a database column should not break every client.</li>
 *   <li><b>Shape.</b> The request carries a plaintext password; the entity stores a hash.
 *       They are genuinely different types.</li>
 * </ul>
 *
 * <p>The annotations are Jakarta Bean Validation. They only run where a parameter is marked
 * {@code @Valid} in the controller - the annotations alone do nothing.
 */
public record RegisterRequest(

        @NotBlank(message = "Full name is required")
        @Size(min = 3, max = 120, message = "Full name must be between 3 and 120 characters")
        String fullName,

        /*
         * @Email checks the shape only. It cannot know whether the mailbox exists - that needs
         * a confirmation email, which is a product decision, not a validation annotation.
         */
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 180, message = "Email is too long")
        String email,

        /*
         * Indian mobile numbers are 10 digits starting 6-9. Validating the format here means a
         * typo is caught before it reaches the crew who have to call this number on move day.
         */
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
        String phone,

        /*
         * Minimum 8 characters. The regex requires at least one letter and one digit:
         *   (?=.*[A-Za-z])  a lookahead - "somewhere ahead there is a letter"
         *   (?=.*\d)        "somewhere ahead there is a digit"
         *   .{8,}           and the whole string is at least 8 characters
         * Lookaheads let us express several independent rules in one pattern without
         * consuming any characters.
         */
        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$",
                message = "Password must be at least 8 characters and contain a letter and a number")
        String password
) {
}
