package com.tamvagbackend.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tamvagbackend.dto.ApiError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import static org.junit.jupiter.api.Assertions.*;

class SecurityExceptionHandlerTest {

    private ObjectMapper objectMapper;
    private SecurityExceptionHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        handler = new SecurityExceptionHandler(objectMapper);
    }

    @Test
    void shouldReturn401WithApiError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/v1/profile"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setAttribute(
                "tamva.requestId",
                "request-401-test"
        );

        handler.commence(
                request,
                response,
                new BadCredentialsException("Invalid credentials")
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED.value(),
                response.getStatus()
        );

        assertEquals(
                "request-401-test",
                response.getHeader("X-Request-Id")
        );

        assertTrue(
                response.getContentType().startsWith("application/json")
        );

        ApiError error = objectMapper.readValue(
                response.getContentAsString(),
                ApiError.class
        );

        assertEquals(401, error.status());
        assertEquals("Unauthorized", error.error());
        assertEquals("Authentication is required", error.message());
        assertEquals("/v1/profile", error.path());
        assertEquals("request-401-test", error.requestId());
    }

    @Test
    void shouldReturn403WithApiError() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/v1/profile"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        request.setAttribute(
                "tamva.requestId",
                "request-403-test"
        );

        handler.handle(
                request,
                response,
                new AccessDeniedException("Insufficient scope")
        );

        assertEquals(
                HttpStatus.FORBIDDEN.value(),
                response.getStatus()
        );

        assertEquals(
                "request-403-test",
                response.getHeader("X-Request-Id")
        );

        assertTrue(
                response.getContentType().startsWith("application/json")
        );

        ApiError error = objectMapper.readValue(
                response.getContentAsString(),
                ApiError.class
        );

        assertEquals(403, error.status());
        assertEquals("Forbidden", error.error());
        assertEquals("Access denied", error.message());
        assertEquals("/v1/profile", error.path());
        assertEquals("request-403-test", error.requestId());
    }

    @Test
    void shouldGenerateRequestIdWhenAttributeAndHeaderAreMissing()
            throws Exception {

        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET",
                "/v1/profile"
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(
                request,
                response,
                new BadCredentialsException("Invalid credentials")
        );

        String requestId = response.getHeader("X-Request-Id");

        assertNotNull(requestId);
        assertFalse(requestId.isBlank());

        ApiError error = objectMapper.readValue(
                response.getContentAsString(),
                ApiError.class
        );

        assertEquals(401, error.status());
        assertEquals(requestId, error.requestId());
    }
}