package com.tamvagbackend.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestIdIntegrationTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void suppliedRequestIdIsReturnedOnSuccessfulResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.addHeader("X-Request-Id", "integration-request-123");

        filter.doFilter(request, response, filterChain);

        assertEquals(
                "integration-request-123",
                response.getHeader("X-Request-Id")
        );

        assertEquals(
                "integration-request-123",
                request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME)
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void requestIdIsGeneratedWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader("X-Request-Id");

        assertNotNull(requestId);
        assertEquals(
                requestId,
                request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME)
        );

        verify(filterChain).doFilter(request, response);
    }
}