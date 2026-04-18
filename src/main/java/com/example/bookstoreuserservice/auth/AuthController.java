package com.example.bookstoreuserservice.auth;

import com.example.bookstoreuserservice.auth.dto.LoginRequest;
import com.example.bookstoreuserservice.auth.dto.LoginResponse;
import com.example.bookstoreuserservice.auth.dto.RegisterRequest;
import com.example.bookstoreuserservice.auth.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(body));
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        return authService.login(body);
    }
}
