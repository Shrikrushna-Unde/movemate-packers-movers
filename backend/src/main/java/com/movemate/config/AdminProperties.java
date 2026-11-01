package com.movemate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The bootstrap admin account, bound from {@code app.admin.*}.
 *
 * <p>Every admin panel has a chicken-and-egg problem: you need an admin to create an admin.
 * The common bad answer is to hardcode a BCrypt hash in a SQL migration, which commits a
 * password to git forever. Instead we read the credentials from configuration and hash them at
 * startup - so production sets {@code ADMIN_PASSWORD} as an env var and git never sees it.
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        String email,
        String password,
        String fullName
) {
}
