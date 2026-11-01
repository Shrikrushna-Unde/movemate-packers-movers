package com.movemate.config;

import com.movemate.security.AppUserDetailsService;
import com.movemate.security.JwtAccessDeniedHandler;
import com.movemate.security.JwtAuthenticationEntryPoint;
import com.movemate.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * The security rulebook: who can call what, and how a request is authenticated.
 *
 * <p>This is the file an interviewer is most likely to open, so every decision below has a
 * reason written next to it.
 */
@Configuration
@EnableWebSecurity
// Switches on @PreAuthorize / @PostAuthorize. We use URL rules as the primary gate AND
// method annotations on admin services - defence in depth, so a future URL refactor that
// accidentally widens a path still cannot reach admin logic.
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint authenticationEntryPoint;
    private final JwtAccessDeniedHandler accessDeniedHandler;
    private final AppUserDetailsService userDetailsService;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint authenticationEntryPoint,
                          JwtAccessDeniedHandler accessDeniedHandler,
                          AppUserDetailsService userDetailsService,
                          CorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.userDetailsService = userDetailsService;
        this.corsProperties = corsProperties;
    }

    /**
     * Password hashing.
     *
     * <p>BCrypt, not SHA-256 or MD5, and the reasons matter:
     * <ul>
     *   <li>it is <b>slow by design</b> (the strength parameter, default 10, means 2^10
     *       rounds), so brute-forcing a stolen database is expensive;</li>
     *   <li>it <b>salts automatically</b> - the salt is stored inside the 60-character output,
     *       so two users with the same password get different hashes and rainbow tables are
     *       useless;</li>
     *   <li>the work factor can be raised as hardware gets faster, without changing the
     *       algorithm.</li>
     * </ul>
     * Hashing is also one-way: {@code matches(raw, hash)} re-hashes the input and compares.
     * There is no "decrypt the password", which is the correct answer when someone asks how to
     * email a user their forgotten password (you cannot - you send a reset link).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * The provider that performs username/password login: it loads the user through
     * {@link AppUserDetailsService} and verifies the password with the encoder above.
     */
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        // Without this, Spring short-circuits the password check when the user is not found,
        // and the different response time reveals which emails exist (a timing attack).
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} so {@code AuthService} can call
     * {@code authenticate(...)} instead of comparing hashes by hand.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * The filter chain. Order of the {@code requestMatchers} calls is significant: the FIRST
     * match wins, so specific rules must come before general ones.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF protection defends against a browser silently replaying a COOKIE.
                // We authenticate with an Authorization header that JavaScript must attach
                // explicitly, so there is nothing for another site to replay - and a stateless
                // API has no CSRF token to keep. Disabling it here is correct; disabling it on
                // a cookie-session app would be a vulnerability.
                .csrf(csrf -> csrf.disable())

                // CORS must be configured in the security chain (not only as a WebMvc setting),
                // otherwise the security filters reject the browser's preflight before Spring
                // MVC ever sees it.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // STATELESS: never create an HttpSession, never read JSESSIONID. Every request
                // must carry its own JWT. This is what makes the API horizontally scalable -
                // any instance can serve any request with no shared session store.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Our JSON 401 instead of a Basic-auth browser popup, and our JSON 403 instead
                // of an empty body. Both are needed because security filters run BEFORE the
                // DispatcherServlet, so @RestControllerAdvice never sees these failures.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))

                .authorizeHttpRequests(auth -> auth
                        // --- Infrastructure ---
                        // Preflight requests carry no credentials by definition, so they must
                        // be allowed or every cross-origin POST fails.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

                        // --- Public reads: the marketing website ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/catalog/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/testimonials").permitAll()

                        // --- Public writes, each one deliberate ---
                        // Login and registration obviously cannot require a login.
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        // Quotes: forcing a signup before showing a price would kill conversion.
                        .requestMatchers(HttpMethod.POST, "/api/v1/quotes/estimate").permitAll()
                        // Guest booking: the whole point of the site.
                        .requestMatchers(HttpMethod.POST, "/api/v1/bookings").permitAll()
                        // Tracking needs reference + phone, which acts as the credential.
                        .requestMatchers(HttpMethod.GET, "/api/v1/bookings/track").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/contact").permitAll()

                        // --- Admin ---
                        // One prefix for every privileged endpoint, so the rule is impossible
                        // to forget when adding a new admin feature.
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        // --- Everything else needs a valid token ---
                        // A deny-by-default tail. Add an endpoint and forget to classify it and
                        // it is protected, not exposed. Never end this chain with permitAll().
                        .anyRequest().authenticated())

                .authenticationProvider(daoAuthenticationProvider())

                // Our JWT filter must run BEFORE the username/password filter so that a request
                // arriving with a token is already authenticated by the time authorisation is
                // evaluated.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS: which browser origins may call this API.
     *
     * <p>CORS is enforced by the BROWSER, not by us - it stops page A from reading responses
     * from API B unless B opts in. It is not a replacement for authentication (curl ignores it
     * entirely), but misconfiguring it is the number one reason a React app that works in
     * Postman fails in Chrome.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // setAllowedOrigins (exact list), never addAllowedOrigin("*") on an API that accepts
        // credentials - the spec forbids combining the wildcard with credentials, and it would
        // let any website on the internet call this API as a logged-in user.
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Authorization and Content-Type are the two we actually need; listing them explicitly
        // documents the contract.
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin"));

        // Lets the browser read Location after a 201 Created.
        configuration.setExposedHeaders(List.of("Location"));

        // false because we send the token in a header, not a cookie. Keeping it off means a
        // malicious site cannot make the browser attach anything on the user's behalf.
        configuration.setAllowCredentials(false);

        // Caches the preflight result for an hour, so the browser stops sending an OPTIONS
        // request before every single call.
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
