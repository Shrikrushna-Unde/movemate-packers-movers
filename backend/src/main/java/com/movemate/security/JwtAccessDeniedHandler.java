package com.movemate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.movemate.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Produces the 403 response when an authenticated user hits an endpoint they are not allowed
 * to use - a customer calling {@code /api/v1/admin/**}, for example.
 *
 * <p>This is the counterpart to {@link JwtAuthenticationEntryPoint} (which handles 401 for
 * anonymous callers), and it exists for the same non-obvious reason: <b>security filters run
 * before the DispatcherServlet</b>, so a denial raised by the URL rules in the filter chain
 * never reaches {@code @RestControllerAdvice}. Without this bean, Spring Security returns a 403
 * with an empty body, while the identical denial raised later by {@code @PreAuthorize} comes
 * back as our normal JSON error. Wiring this handler makes both paths produce the same shape,
 * which is the whole point of having one error contract.
 *
 * <p>Note the distinction worth getting right in an interview: <b>401 means "I do not know who
 * you are"</b> and <b>403 means "I know who you are and the answer is no"</b>. Returning 401
 * for an authorisation failure invites the client to retry authentication, which will never
 * help.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public JwtAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiError error = ApiError.of(
                HttpStatus.FORBIDDEN.value(),
                "Forbidden",
                "You do not have permission to perform this action",
                request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
