package com.movemate.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

/**
 * One place that turns every exception into a predictable JSON error.
 *
 * <p>{@code @RestControllerAdvice} is {@code @ControllerAdvice} + {@code @ResponseBody}: it
 * registers these handlers across every controller in the application. The alternative -
 * try/catch in each controller method - duplicates the same block dozens of times and still
 * misses the exceptions thrown by Spring itself before your code is reached.
 *
 * <p>Handler selection is by most-specific type, so the catch-all {@code Exception} method at
 * the bottom only runs when nothing above it matches.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * SLF4J is a facade; Spring Boot wires Logback behind it. Declared static final because a
     * logger is shared, immutable infrastructure - one instance per class, not per request.
     */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Bean Validation failures from a {@code @Valid @RequestBody}.
     *
     * <p>Spring throws this before the controller body executes. We flatten it into
     * {@code {"customerPhone": "Enter a valid 10-digit Indian mobile number"}} so React can
     * attach each message to the field that caused it instead of showing one blob.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
                                                     HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            // putIfAbsent: a field with two broken rules keeps the first message rather than
            // whichever one Hibernate Validator happened to evaluate last.
            fieldErrors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(ApiError.validation(
                HttpStatus.BAD_REQUEST.value(),
                "Please correct the highlighted fields",
                request.getRequestURI(),
                fieldErrors));
    }

    /**
     * Validation failures on method PARAMETERS (a {@code @NotBlank @RequestParam}) rather than
     * on a request body.
     *
     * <p>These arrive as a different exception type because they are raised by a Spring AOP
     * proxy around the {@code @Validated} controller, not by the argument resolver. Handling
     * only {@code MethodArgumentNotValidException} is a very common gap - the result is a 500
     * where a 400 belongs.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex,
                                                              HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            // The property path looks like "track.reference"; the last node is the parameter name.
            String path = violation.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.putIfAbsent(field, violation.getMessage());
        }
        return ResponseEntity.badRequest().body(ApiError.validation(
                HttpStatus.BAD_REQUEST.value(),
                "Please correct the request parameters",
                request.getRequestURI(),
                fieldErrors));
    }

    /** Unparseable JSON, or a string that is not a valid enum constant. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex,
                                                     HttpServletRequest request) {
        // Debug, not warn: a malformed body is the caller's problem, and logging the raw
        // parser message at WARN would let anyone fill the production log by sending junk.
        log.debug("Malformed request body on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Request body is malformed or contains an unsupported value",
                request.getRequestURI()));
    }

    /** e.g. {@code ?status=BANANA} where the parameter is a BookingStatus. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                       HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "Invalid value '%s' for parameter '%s'".formatted(ex.getValue(), ex.getName()),
                request.getRequestURI()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex,
                                                     HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()));
    }

    /**
     * Illegal booking status transitions, which {@code Booking.changeStatus} raises.
     * The entity protects its own invariants with a plain Java exception; this adapter is the
     * only thing that knows it becomes a 400.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException ex,
                                                       HttpServletRequest request) {
        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                request.getRequestURI()));
    }

    /**
     * Wrong email or password.
     *
     * <p>The message is deliberately vague. Replying "no account with that email" would let an
     * attacker enumerate which addresses are registered - a real finding in security reviews.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex,
                                                         HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Invalid email or password",
                request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "This account has been deactivated. Please contact support.",
                request.getRequestURI()));
    }

    /** Authenticated, but not allowed - e.g. a customer calling an /admin endpoint. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to perform this action",
                request.getRequestURI()));
    }

    /**
     * A database constraint fired - almost always our UNIQUE index catching a duplicate that
     * slipped past the application check under concurrency. Mapping it to 409 instead of 500
     * means the race condition produces a correct, useful response.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex,
                                                        HttpServletRequest request) {
        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                "This record conflicts with existing data",
                request.getRequestURI()));
    }

    /**
     * Last resort. Two rules here:
     * <ol>
     *   <li>Log the full stack trace server-side - this is the only place it exists.</li>
     *   <li>Return a generic sentence to the client. Stack traces expose class names, library
     *       versions and sometimes SQL, which is free reconnaissance for an attacker.</li>
     * </ol>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Something went wrong on our side. Please try again.",
                request.getRequestURI()));
    }
}
