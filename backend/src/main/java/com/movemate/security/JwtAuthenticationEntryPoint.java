package com.movemate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movemate.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Produces the 401 response when an anonymous caller hits a protected endpoint.
 *
 * <p>Without this bean, Spring Security's default entry point sends
 * {@code WWW-Authenticate: Basic} and an empty body - which makes a browser pop up a native
 * login dialog over our React app. For a JSON API we want a JSON body and no dialog.
 *
 * <p>This is also where {@link com.movemate.exception.GlobalExceptionHandler} cannot help:
 * security filters run BEFORE the DispatcherServlet, so no {@code @ControllerAdvice} ever sees
 * these failures. That is a genuinely useful thing to know - it explains why a 401 from a
 * filter looks different from a 400 from a controller unless you wire this up.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /** Injected rather than created per call: ObjectMapper is thread-safe but expensive to build. */
    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                "Authentication required. Please sign in and try again.",
                request.getRequestURI());

        // Writing straight to the raw response stream, because at this point in the chain
        // there is no HttpMessageConverter to do it for us.
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
