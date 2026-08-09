package com.workpulsetracker.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * JWT-фильтр только для agent API. Не трогает session-auth веб-кабинета.
 */
@Component
public class AgentJwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AgentJwtAuthenticationFilter.class);
    private static final String schema = "public";
    private static final String BEARER_PREFIX = "Bearer ";

    private final AgentJwtService agentJwtService;

    public AgentJwtAuthenticationFilter(AgentJwtService agentJwtService) {
        this.agentJwtService = agentJwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (StringUtils.isBlank(requestUri)) {
            return true;
        }
        if (requestUri.equals("/api/agent/auth") || requestUri.startsWith("/api/agent/auth/")) {
            return true;
        }
        return !(requestUri.startsWith("/api/agent/telemetry")
                || requestUri.startsWith("/api/agent/sync")
                || requestUri.startsWith("/api/agent/feedback"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (Objects.nonNull(SecurityContextHolder.getContext().getAuthentication())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.isBlank(authorizationHeader) || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (StringUtils.isBlank(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            AgentDevicePrincipal agentDevicePrincipal = agentJwtService.parsePrincipal(token);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    agentDevicePrincipal,
                    null,
                    agentDevicePrincipal.getAuthorities()
            );
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (Exception exception) {
            logger.info("schema={} Invalid agent JWT: {}", schema, exception.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
