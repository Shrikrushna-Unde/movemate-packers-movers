package com.movemate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Binds {@code app.cors.allowed-origins}.
 *
 * <p>Spring converts a comma-separated property value into a {@code List<String>} for us, so
 * one env var on Railway can carry several origins:
 * {@code CORS_ALLOWED_ORIGINS=https://movemate.vercel.app,https://www.movemate.in}
 *
 * <p>Why externalise this at all? Because the allowed origin differs per environment
 * (localhost:5173 while developing, the real domain in production) and hardcoding it would
 * mean a code change and a rebuild to move the frontend.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}
