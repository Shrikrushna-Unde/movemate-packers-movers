package com.movemate.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe binding of the {@code app.jwt.*} properties.
 *
 * <p>{@code @ConfigurationProperties} beats scattering {@code @Value("${app.jwt.secret}")}
 * across classes because:
 * <ul>
 *   <li>the settings are grouped into one object you can inject and test;</li>
 *   <li>binding happens once at startup, so a typo or missing value fails fast instead of
 *       surfacing as a null the first time someone logs in;</li>
 *   <li>types are converted for you - {@code expirationMinutes} arrives as a long.</li>
 * </ul>
 *
 * <p>Spring Boot 3 binds records through their canonical constructor with no extra annotation.
 * The record is picked up by {@code @ConfigurationPropertiesScan} on the main class.
 *
 * @param secret            HMAC key. Minimum 32 bytes for HS256 - {@code Keys.hmacShaKeyFor}
 *                          throws if it is shorter, which is the library refusing to let you
 *                          build something insecure.
 * @param expirationMinutes token lifetime
 * @param issuer            the {@code iss} claim; we also require it when parsing, so a token
 *                          minted by another system with the same key is still rejected
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long expirationMinutes,
        String issuer
) {
}
