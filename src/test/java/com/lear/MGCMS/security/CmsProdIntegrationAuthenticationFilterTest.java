package com.lear.MGCMS.security;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.servlet.ServletException;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class CmsProdIntegrationAuthenticationFilterTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validKey_authenticatesSequenceStartAsProcess() throws ServletException, IOException {
        CmsProdIntegrationAuthenticationFilter filter = new CmsProdIntegrationAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "apiKey", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sequence/SEQ1/start");
        request.addHeader(CmsProdIntegrationAuthenticationFilter.HEADER, "secret");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_PROCESS".equals(a.getAuthority())));
    }

    @Test
    void wrongKey_doesNotAuthenticate() throws ServletException, IOException {
        CmsProdIntegrationAuthenticationFilter filter = new CmsProdIntegrationAuthenticationFilter();
        ReflectionTestUtils.setField(filter, "apiKey", "secret");
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/sequence/SEQ1/start");
        request.addHeader(CmsProdIntegrationAuthenticationFilter.HEADER, "wrong");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
