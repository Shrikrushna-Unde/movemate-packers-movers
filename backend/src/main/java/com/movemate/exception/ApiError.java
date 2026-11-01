package com.movemate.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * One consistent error shape for the entire API.
 *
 * <p>A client that can rely on {@code { status, error, message, fieldErrors }} for every
 * failure can write one error handler. Without this, Spring returns three different shapes -
 * its own Whitelabel error, a validation dump, and a security 403 - and the React code ends up
 * full of defensive checks.
 *
 * <p>{@code @JsonInclude(NON_NULL)} drops null fields from the JSON, so a plain 404 does not
 * carry an empty {@code fieldErrors} object.
 *
 * @param timestamp   when it happened, for correlating with server logs
 * @param status      HTTP status code, repeated in the body because fetch/axios error handling
 *                    is easier when it is in one place
 * @param error       the status reason phrase, e.g. "Not Found"
 * @param message     a sentence safe to show a user
 * @param path        which endpoint failed
 * @param fieldErrors field name to message, present only for validation failures
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError validation(int status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, "Bad Request", message, path, fieldErrors);
    }
}
