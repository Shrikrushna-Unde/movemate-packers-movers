package com.movemate.dto;

/**
 * What {@code /auth/login} and {@code /auth/register} return.
 *
 * <p>The token and the user profile are returned together on purpose. The React app needs the
 * role immediately to decide whether to show the admin menu; without the profile it would have
 * to decode the JWT in the browser or make a second round trip just to render the navbar.
 *
 * @param token       the signed JWT the client sends back as {@code Authorization: Bearer <token>}
 * @param tokenType   always "Bearer" - part of the RFC 6750 contract, kept explicit so clients
 *                    do not hardcode it
 * @param expiresInMs lifetime in milliseconds, so the frontend can pre-emptively log the user
 *                    out instead of discovering expiry through a surprise 401
 * @param user        the authenticated profile, minus anything sensitive
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        UserResponse user
) {

    public static AuthResponse of(String token, long expiresInMs, UserResponse user) {
        return new AuthResponse(token, "Bearer", expiresInMs, user);
    }
}
