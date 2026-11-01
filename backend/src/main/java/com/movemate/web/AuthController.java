package com.movemate.web;

import com.movemate.dto.AuthResponse;
import com.movemate.dto.LoginRequest;
import com.movemate.dto.RegisterRequest;
import com.movemate.dto.UserResponse;
import com.movemate.security.AuthenticatedUser;
import com.movemate.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints.
 *
 * <p>{@code @RestController} = {@code @Controller} + {@code @ResponseBody}: every return value
 * is serialised to JSON by Jackson instead of being resolved as a view name.
 *
 * <p>{@code @RequestMapping} at class level sets the shared prefix, so each method only
 * declares its own segment. The {@code /v1} in the path is API versioning - the cheapest
 * insurance you can buy. When a breaking change is needed you add {@code /v2} and leave
 * {@code /v1} serving the mobile app that has not been updated yet.
 *
 * <p>Notice how thin these methods are. A controller should read the request, call one service
 * method and choose a status code. If you find business logic in a controller, it cannot be
 * reused or unit-tested without spinning up HTTP.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, sign in, and read the current profile")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * {@code @Valid} is what actually triggers Bean Validation on the DTO. Without it, every
     * {@code @NotBlank} and {@code @Pattern} in {@link RegisterRequest} is inert - a mistake
     * that is invisible until someone registers with an empty name.
     *
     * <p>201 Created, not 200: a new resource now exists. Using the right status code is free
     * and it is the first thing a reviewer checks in a REST API.
     */
    @PostMapping("/register")
    @Operation(summary = "Create a customer account and return a JWT")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * 200 OK, because login creates no resource - it exchanges credentials for a token.
     *
     * <p>POST rather than GET even though it only reads: the credentials must be in the body.
     * A GET would put the password in the URL, where it lands in browser history, proxy logs
     * and the server access log.
     */
    @PostMapping("/login")
    @Operation(summary = "Exchange email and password for a JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Returns the caller's own profile.
     *
     * <p>{@code @AuthenticationPrincipal} injects the principal that
     * {@link com.movemate.security.JwtAuthenticationFilter} put into the SecurityContext. This
     * endpoint takes NO user id parameter, and that is the whole point: an id in the path
     * ({@code GET /users/{id}}) would need an ownership check, and forgetting that check is
     * how one customer reads another customer's data. Deriving identity from the verified
     * token makes the bug impossible.
     */
    @GetMapping("/me")
    @Operation(summary = "Current user profile, derived from the bearer token")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ResponseEntity.ok(authService.currentUser(currentUser.getId()));
    }
}
