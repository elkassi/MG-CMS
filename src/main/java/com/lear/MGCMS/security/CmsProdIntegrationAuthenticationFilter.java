package com.lear.MGCMS.security;

import java.io.IOException;
import java.util.Collections;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Narrow server-to-server auth for CMS-Prod notifying MG-CMS that spreading
 * started. This avoids making {@code POST /api/sequence/{seq}/start} public
 * while letting the production app authenticate without an MG-CMS user JWT.
 */
@Component
public class CmsProdIntegrationAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-MG-CMS-Integration-Key";

    @Value("${mgcms.integration.cms-prod.api-key:}")
    private String apiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isSequenceStart(request)
                && SecurityContextHolder.getContext().getAuthentication() == null
                && StringUtils.hasText(apiKey)
                && apiKey.equals(request.getHeader(HEADER))) {
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "cms-prod-integration",
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_PROCESS")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }

    private boolean isSequenceStart(HttpServletRequest request) {
        String path = request.getRequestURI();
        return HttpMethod.POST.matches(request.getMethod())
                && path != null
                && path.startsWith(request.getContextPath() + "/api/sequence/")
                && path.endsWith("/start");
    }
}
