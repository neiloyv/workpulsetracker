package com.timetracker.server.security;

import com.timetracker.server.config.AppProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(GoogleOAuth2SuccessHandler.class);

    private final AuthUserService authUserService;
    private final AppProperties appProperties;

    public GoogleOAuth2SuccessHandler(AuthUserService authUserService, AppProperties appProperties) {
        this.authUserService = authUserService;
        this.appProperties = appProperties;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        authUserService.resolveOrCreateFromOAuth(oAuth2User);
        String redirectUrl = appProperties.getUiOrigin() + "/auth/callback";
        logger.info("OAuth login success, redirecting to UI");
        response.sendRedirect(redirectUrl);
    }
}
