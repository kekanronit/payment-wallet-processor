package com.example.payment_wallet_processor.controller;

import com.example.payment_wallet_processor.dto.LoginRequest;
import com.example.payment_wallet_processor.dto.LoginResponse;
import com.example.payment_wallet_processor.dto.RegisterRequest;
import com.example.payment_wallet_processor.entity.User;
import com.example.payment_wallet_processor.security.JwtService;
import com.example.payment_wallet_processor.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(
            AuthService authService,
            JwtService jwtService) {

        this.authService = authService;
        this.jwtService = jwtService;
    }

    // Register
    @PostMapping("/register")   
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body("User registered successfully");
    }

    // Login
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        // 1. Authenticate User
        User user = authService.login(request);

        // 2. Generate JWT
        String token = jwtService.generateToken(user);

        // 3. Create Response
        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                user.getEmail(),
                user.getRole().name()
        );

        // 4. Return Response
        return ResponseEntity.ok(response);
    }
}