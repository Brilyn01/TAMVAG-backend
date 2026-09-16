package com.tamvagbackend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamvagbackend.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

@Component
public class SecurityExceptionHandler
        implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_ID_ATTRIBUTE = "tamva.requestId";

    private final ObjectMapper objectMapper;

    public SecurityExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        writeError(
                request,
                response,
                HttpStatus.UNAUTHORIZED,
                "Authentication is required"
        );
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        writeError(
                request,
                response,
                HttpStatus.FORBIDDEN,
                "Access denied"
        );
    }

    private void writeError(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {

        String requestId = resolveRequestId(request);

        ApiError error = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                requestId,
                null
        );

        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(REQUEST_ID_HEADER, requestId);

        objectMapper.writeValue(response.getWriter(), error);
    }

    private String resolveRequestId(HttpServletRequest request) {
        Object attribute = request.getAttribute(REQUEST_ID_ATTRIBUTE);

        if (attribute instanceof String requestId && !requestId.isBlank()) {
            return requestId;
        }

        String requestId = request.getHeader(REQUEST_ID_HEADER);

        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        return requestId.trim();
    }
}
