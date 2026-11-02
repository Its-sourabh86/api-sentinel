package com.apisentinel.controller;

import com.apisentinel.dto.request.LoginRequest;
import com.apisentinel.dto.request.RegisterRequest;
import com.apisentinel.dto.response.LoginResponse;
import com.apisentinel.dto.response.UserResponse;
import com.apisentinel.security.AuthRateLimiter;
import com.apisentinel.service.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRateLimiter rateLimiter;

    public AuthController(AuthService authService, AuthRateLimiter rateLimiter) {
        this.authService = authService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request,
            HttpServletRequest http) {
        rateLimiter.checkRegister(http.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
            HttpServletRequest http) {
        rateLimiter.checkLogin(http.getRemoteAddr(), request.email());
        return ResponseEntity.ok(authService.login(request));
    }
}