package com.movemate.security;

import com.movemate.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Creates and verifies JWTs.
 *
 * <p><b>What a JWT actually is.</b> Three base64url segments separated by dots:
 * {@code header.payload.signature}. The header says which algorithm; the payload holds the
 * claims (subject, issuer, expiry, plus anything we add); the signature is
 * {@code HMAC-SHA256(header + "." + payload, secret)}.
 *
 * <p><b>What that buys us.</b> Any tampering changes the payload, which invalidates the
 * signature, and only the server knows the secret. So the server can trust the token without
 * storing a session - which is why this scales horizontally and why it suits a React SPA
 * talking to a stateless API.
 *
 * <p><b>What it does NOT buy us.</b> The payload is encoded, not encrypted: anyone can paste
 * a token into jwt.io and read the claims. Never put anything secret in a JWT. And because
 * there is no server-side session, a token cannot be revoked before it expires - the standard
 * answers are short expiry plus refresh tokens, or a denylist in Redis. Say this honestly in
 * an interview; claiming JWTs are revocable is a red flag.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Custom claim names. Kept as constants so a typo cannot silently create a second claim. */
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_NAME = "name";

    private final JwtProperties properties;

    /**
     * The signing key, derived once at startup.
     *
     * <p>{@code final} and built in the constructor because key derivation is pure overhead to
     * repeat per request, and an immutable key makes this service thread-safe - which matters,
     * since Spring beans are singletons serving many concurrent requests.
     */
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        // hmacShaKeyFor validates the length and throws WeakKeyException for anything under
        // 256 bits. Let it: a short secret is a vulnerability, not an inconvenience.
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Mints a token for a successfully authenticated user.
     *
     * <p>The role is embedded as a claim so authorisation needs no database read on every
     * request. The tradeoff: if an admin is demoted, their existing token still says
     * ROLE_ADMIN until it expires. We accept that for a 24-hour token and mitigate it by
     * re-loading the user in {@link JwtAuthenticationFilter}, which is where the authoritative
     * role actually comes from.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(properties.expirationMinutes()));

        return Jwts.builder()
                // `sub` - who the token is about. Email, because that is our login identifier.
                .subject(user.getEmail())
                // `iss` - who minted it. Verified on parse.
                .issuer(properties.issuer())
                // `iat` / `exp` - issued-at and expiry, both standard registered claims.
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                // Private claims. Handy for the client, never trusted for authorisation.
                .claim(CLAIM_USER_ID, user.getId())
                .claim(CLAIM_ROLE, user.getRole().name())
                .claim(CLAIM_NAME, user.getFullName())
                // Signs with HS256, inferred from the key type.
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies a token.
     *
     * @return the claims, or {@code null} when the token is invalid for any reason
     *         (bad signature, expired, wrong issuer, malformed)
     */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    // Checks the signature against our secret. Without this line you would be
                    // trusting whatever the client claims - the classic "alg: none" JWT attack.
                    .verifyWith(key)
                    .requireIssuer(properties.issuer())
                    .build()
                    // parseSignedClaims rejects an unsigned token outright, and throws
                    // ExpiredJwtException once `exp` has passed.
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            // DEBUG, not ERROR: an expired token is routine - every user hits it daily.
            // Logging it at ERROR would bury real problems in noise.
            log.debug("Rejected JWT: {}", ex.getMessage());
            return null;
        }
    }

    /** @return the email ({@code sub} claim), or null when the token is not valid. */
    public String extractEmail(String token) {
        Claims claims = parseClaims(token);
        return claims == null ? null : claims.getSubject();
    }

    /** Token lifetime in milliseconds, returned to the client in {@code AuthResponse}. */
    public long expirationMillis() {
        return Duration.ofMinutes(properties.expirationMinutes()).toMillis();
    }
}
