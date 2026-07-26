package com.workpulsetracker.server.web;

import com.workpulsetracker.server.security.AuthUserService;
import com.workpulsetracker.server.web.dto.LoginRequest;
import com.workpulsetracker.server.web.dto.MeResponse;
import com.workpulsetracker.server.web.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUserService authUserService;

    public AuthController(AuthUserService authUserService) {
        this.authUserService = authUserService;
    }

    @PostMapping("/register")
    public MeResponse register(
            @Valid @RequestBody RegisterRequest registerRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authUserService.register(registerRequest, httpServletRequest, httpServletResponse);
    }

    @PostMapping("/login")
    public MeResponse login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        return authUserService.login(loginRequest, httpServletRequest, httpServletResponse);
    }
}
