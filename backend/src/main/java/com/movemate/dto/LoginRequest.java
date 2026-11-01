package com.movemate.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login payload.
 *
 * <p>Note what is NOT here: no password complexity rules. Validation on login must be minimal,
 * because the rules may have changed since the account was created - rejecting an old but
 * correct password with a 400 would lock out legitimate users. Only presence is checked;
 * correctness is the authentication manager's job.
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
