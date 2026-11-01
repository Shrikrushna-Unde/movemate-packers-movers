package com.movemate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Turns a {@code Authorization: Bearer <jwt>} header into an authenticated SecurityContext.
 *
 * <p>This runs on every request, before the controller. The flow:
 * <ol>
 *   <li>read the Authorization header; if there is no Bearer token, do nothing and continue;</li>
 *   <li>verify the signature and expiry;</li>
 *   <li>load the user from the database;</li>
 *   <li>put an {@code Authentication} into the {@link SecurityContextHolder}.</li>
 * </ol>
 *
 * <p><b>Why extend {@link OncePerRequestFilter}?</b> A plain servlet filter can run several
 * times for one request - on a forward, an include, or an async dispatch - which would redo
 * this work and can produce confusing double-authentication. OncePerRequestFilter guarantees a
 * single execution per request.
 *
 * <p><b>Why never reject here?</b> This filter's only job is "authenticate if possible". If the
 * token is missing or bad it leaves the context empty and calls the chain; a later
 * authorisation filter decides whether anonymous access is acceptable for that URL. That
 * separation is what lets {@code POST /bookings} be public while {@code GET /bookings/me}
 * requires a login, with no if-statements about URLs in this class.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = extractToken(request);

        // Two guards before doing any work:
        //  - no token: an anonymous request, which is legitimate for public endpoints
        //  - context already populated: never overwrite an existing authentication
        if (token == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractEmail(token);
        if (email != null) {
            try {
                // Deliberate database read per request.
                //   Cost:    one indexed SELECT on users.
                //   Benefit: the role and the enabled flag are authoritative, so deactivating
                //            an account takes effect immediately instead of when the token
                //            expires. Trusting the role claim alone would be faster and wrong.
                //   At scale you would cache this (Spring Cache / Redis) rather than drop it.
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (userDetails.isEnabled()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    // Credentials are null: the JWT already proved identity, and
                                    // keeping a password in the context would be a leak risk.
                                    null,
                                    userDetails.getAuthorities());

                    // Attaches the remote IP and session id - used by auditing and rate limiting.
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // SecurityContextHolder is ThreadLocal-backed, so from here on
                    // @AuthenticationPrincipal and @PreAuthorize can see this user.
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException ex) {
                // A valid, unexpired token for an account that has since been deleted.
                // Stay anonymous; do not throw. The authorisation layer will issue 401/403.
                logger.debug("Token references a user that no longer exists: " + email);
            }
        }

        // Always continue the chain. Returning without this line would silently
        // swallow the request and the client would hang waiting for a response.
        filterChain.doFilter(request, response);
    }

    /**
     * Pulls the raw token out of the header.
     *
     * @return the token, or null when the header is absent or not a Bearer header
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
