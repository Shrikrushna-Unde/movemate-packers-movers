package com.movemate.exception;

/**
 * Thrown when a request is well-formed but collides with existing state:
 * registering an email that is already taken, or reusing a service slug.
 *
 * <p>Maps to HTTP 409 Conflict. The distinction from 400 matters to a client: a 400 means
 * "fix your input format", a 409 means "your input is fine, but the world has changed" -
 * which is why the React signup form can show "this email is already registered" for 409 and
 * a generic field error for 400.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
