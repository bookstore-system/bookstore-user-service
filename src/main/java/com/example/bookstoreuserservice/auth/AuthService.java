package com.example.bookstoreuserservice.auth;

import com.example.bookstoreuserservice.auth.dto.LoginRequest;
import com.example.bookstoreuserservice.auth.dto.LoginResponse;
import com.example.bookstoreuserservice.auth.dto.RegisterRequest;
import com.example.bookstoreuserservice.auth.dto.UserResponse;
import com.example.bookstoreuserservice.exception.EmailAlreadyInUseException;
import com.example.bookstoreuserservice.security.JwtService;
import com.example.bookstoreuserservice.user.User;
import com.example.bookstoreuserservice.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyInUseException("Email is already registered");
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }
        userRepository.save(user);
        return toResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(
                                () ->
                                        new BadCredentialsException("Invalid email or password"));
        String token = jwtService.generateToken(user.getEmail());
        return new LoginResponse(
                token, "Bearer", jwtService.getExpirationSeconds(), toResponse(user));
    }

    private static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(), user.getEmail(), user.getFullName(), user.getCreatedAt());
    }
}
