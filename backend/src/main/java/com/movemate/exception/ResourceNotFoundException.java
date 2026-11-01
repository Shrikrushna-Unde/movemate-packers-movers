package com.movemate.exception;

/**
 * Thrown when a requested record does not exist.
 *
 * <p>It extends {@link RuntimeException}, not {@code Exception}. That is a conscious choice:
 * a checked exception would force {@code throws} declarations up through every service and
 * controller, and there is nothing a caller can do to recover from "this booking is not here"
 * except return 404. Spring's own {@code DataAccessException} hierarchy is unchecked for the
 * same reason.
 *
 * <p>It carries no HTTP concepts. The mapping to 404 lives in
 * {@link GlobalExceptionHandler}, so the service layer stays usable from a scheduled job or a
 * CLI where HTTP is meaningless. (Spring's {@code @ResponseStatus} annotation would work too,
 * but it welds the exception to the web layer.)
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Builds a consistent message, e.g. {@code Booking not found with reference 'MM-2026-X'}.
     *
     * @param resource  what we looked for ("Booking")
     * @param fieldName which field we looked it up by ("reference")
     * @param value     the value we used
     */
    public ResourceNotFoundException(String resource, String fieldName, Object value) {
        super("%s not found with %s '%s'".formatted(resource, fieldName, value));
    }
}
