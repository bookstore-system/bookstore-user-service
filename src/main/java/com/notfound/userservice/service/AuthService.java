package com.notfound.userservice.service;

import com.notfound.userservice.model.dto.request.ChangePasswordRequest;
import com.notfound.userservice.model.dto.request.LoginRequest;
import com.notfound.userservice.model.dto.request.RegisterRequest;
import com.notfound.userservice.model.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);

    String generateEmailVerificationToken(String email);

    String validateEmailVerificationToken(String token);

    void changePassword(String username, ChangePasswordRequest request);

    void resetPassword(String email, String newPassword);

    AuthResponse refreshToken(String refreshToken);

}
