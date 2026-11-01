package com.movemate.exception;

/**
 * Thrown for business rules that Bean Validation annotations cannot express -
 * for example "a household move needs a home size" or "insurance needs a declared value",
 * where validity depends on another field's value.
 *
 * <p>Maps to HTTP 400 in {@link GlobalExceptionHandler}.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
