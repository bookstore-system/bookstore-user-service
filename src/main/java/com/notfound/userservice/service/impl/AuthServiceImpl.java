package com.notfound.userservice.service.impl;

import com.notfound.userservice.exception.EmailAlreadyInUseException;
import com.notfound.userservice.model.dto.request.ChangePasswordRequest;
import com.notfound.userservice.model.dto.request.LoginRequest;
import com.notfound.userservice.model.dto.request.RegisterRequest;
import com.notfound.userservice.model.dto.response.AuthResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.enums.Role;
import com.notfound.userservice.model.mapper.UserMapper;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.security.JwtService;
import com.notfound.userservice.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new EmailAlreadyInUseException("Username is already taken");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailAlreadyInUseException("Email is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .dateOfBirth(request.getDateOfBirth())
                .role(Role.CUSTOMER)
                .status("active")
                .build();

        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), java.util.Map.of(
                "role", user.getRole().name(),
                "userId", user.getId().toString()
        ));
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        UserResponse userResponse = userMapper.toUserResponse(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        String token = jwtService.generateToken(user.getUsername(), java.util.Map.of(
                "role", user.getRole().name(),
                "userId", user.getId().toString()
        ));
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());
        UserResponse userResponse = userMapper.toUserResponse(user);

        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userResponse)
                .build();
    }

    @Override
    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BadCredentialsException("Old password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfimPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @jakarta.transaction.Transactional
    public void resetPassword(String email, String newPassword) {
        log.info("Attempting to reset password for email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại với email: " + email));
        
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("Mật khẩu mới không được để trống");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Successfully reset password for user: {}", email);
    }

    @Override
    public String generateEmailVerificationToken(String email) {
        // TODO: Implement email verification token generation
        return jwtService.generateToken(email);
    }

    @Override
    public String validateEmailVerificationToken(String token) {
        // TODO: Implement email verification token validation
        return jwtService.extractSubject(token);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        // TODO: Implement refresh token logic
        String username = jwtService.extractSubject(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        String newToken = jwtService.generateToken(user.getUsername(), java.util.Map.of(
                "role", user.getRole().name(),
                "userId", user.getId().toString()
        ));
        String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());
        UserResponse userResponse = userMapper.toUserResponse(user);

        return AuthResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .user(userResponse)
                .build();
    }


}
