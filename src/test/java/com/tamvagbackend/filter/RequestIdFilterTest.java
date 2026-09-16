package com.tamvagbackend.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    void shouldGenerateRequestIdWhenHeaderIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        String requestId = response.getHeader(RequestIdFilter.HEADER_NAME);

        assertNotNull(requestId);
        assertFalse(requestId.isBlank());

        assertEquals(
                requestId,
                request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME)
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldPreserveProvidedRequestId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        request.addHeader(
                RequestIdFilter.HEADER_NAME,
                "  client-request-123  "
        );

        filter.doFilter(request, response, filterChain);

        assertEquals(
                "client-request-123",
                response.getHeader(RequestIdFilter.HEADER_NAME)
        );

        assertEquals(
                "client-request-123",
                request.getAttribute(RequestIdFilter.ATTRIBUTE_NAME)
        );

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldGenerateDifferentRequestIdsForDifferentRequests() throws Exception {
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        MockHttpServletResponse response1 = new MockHttpServletResponse();
        FilterChain filterChain1 = mock(FilterChain.class);

        MockHttpServletRequest request2 = new MockHttpServletRequest();
        MockHttpServletResponse response2 = new MockHttpServletResponse();
        FilterChain filterChain2 = mock(FilterChain.class);

        filter.doFilter(request1, response1, filterChain1);
        filter.doFilter(request2, response2, filterChain2);

        String requestId1 = response1.getHeader(RequestIdFilter.HEADER_NAME);
        String requestId2 = response2.getHeader(RequestIdFilter.HEADER_NAME);

        assertNotNull(requestId1);
        assertNotNull(requestId2);
        assertNotEquals(requestId1, requestId2);
    }
}
