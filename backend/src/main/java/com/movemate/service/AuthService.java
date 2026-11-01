package com.movemate.service;

import com.movemate.domain.Role;
import com.movemate.domain.User;
import com.movemate.dto.AuthResponse;
import com.movemate.dto.LoginRequest;
import com.movemate.dto.RegisterRequest;
import com.movemate.dto.UserResponse;
import com.movemate.exception.ConflictException;
import com.movemate.exception.ResourceNotFoundException;
import com.movemate.repository.UserRepository;
import com.movemate.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration and login.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    /**
     * Creates a customer account and returns a token, so the user is logged in immediately
     * instead of being bounced to a login form they have just filled in.
     *
     * <p>{@code @Transactional} makes the whole method one database transaction. Here that is
     * mostly about correctness of intent, but it is the habit to build: any method that writes
     * more than one row needs it, or a failure halfway leaves the database inconsistent.
     *
     * <p>Note the hardcoded {@code Role.ROLE_CUSTOMER}. The role is NOT read from the request -
     * a public endpoint that accepted a role would let anyone register as an admin. Promotion
     * happens through an admin action or a database update, never through self-service.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Normalising to lower case means Rohan@x.com and rohan@x.com cannot both register.
        // The email local-part is technically case-sensitive per RFC, but no real mail
        // provider treats it that way, and allowing both creates duplicate-account support
        // tickets forever.
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            // A clean 409 rather than letting the UNIQUE constraint throw a 500-shaped error.
            throw new ConflictException("An account with this email already exists. Please sign in instead.");
        }

        User user = new User(
                request.fullName().trim(),
                email,
                request.phone().trim(),
                // The plaintext password exists only inside this expression and is never
                // logged, stored or returned.
                passwordEncoder.encode(request.password()),
                Role.ROLE_CUSTOMER);

        User saved = userRepository.save(user);
        log.info("Registered new customer with id {}", saved.getId());

        return AuthResponse.of(
                jwtService.generateToken(saved),
                jwtService.expirationMillis(),
                UserResponse.from(saved));
    }

    /**
     * Verifies credentials and issues a token.
     *
     * <p>We delegate to {@link AuthenticationManager} rather than calling
     * {@code passwordEncoder.matches(...)} ourselves. That buys us, for free: the
     * disabled-account check, the timing-attack protection configured on the provider, and any
     * future authentication method (LDAP, OAuth) plugging in without touching this method.
     *
     * <p>A wrong password throws {@code BadCredentialsException}, which
     * {@code GlobalExceptionHandler} maps to a deliberately vague 401.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        // Reached only when authentication succeeded, so the account must exist; the
        // orElseThrow is a guard against a race with account deletion, not an expected path.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        log.info("Login successful for user id {}", user.getId());

        return AuthResponse.of(
                jwtService.generateToken(user),
                jwtService.expirationMillis(),
                UserResponse.from(user));
    }

    /**
     * Returns the current profile. The frontend calls this on page refresh to re-hydrate the
     * logged-in state from a stored token, rather than trusting a cached copy in localStorage
     * that may be stale or tampered with.
     */
    @Transactional(readOnly = true)
    public UserResponse currentUser(Long userId) {
        return userRepository.findById(userId)
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }
}
