package com.notfound.userservice.controller;

import com.notfound.userservice.model.dto.request.*;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.AuthResponse;
import com.notfound.userservice.model.dto.response.IntrospectResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.mapper.UserMapper;
import com.notfound.userservice.service.AuthService;
import com.notfound.userservice.service.OtpService;
import com.notfound.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller xử lý các chức năng xác thực và phân quyền
 * Bao gồm đăng ký, đăng nhập, đổi mật khẩu, quên mật khẩu và xác thực email
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Auth", description = "Đăng ký, đăng nhập, refresh token, OTP, xác thực email, đổi mật khẩu")
public class AuthController {

    AuthService authService;
    UserService userService;
    OtpService otpService;

    /**
     * Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Đăng ký thành công")
                .result(authResponse)
                .build();
    }

    /**
     * Đăng nhập vào hệ thống
     */
    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công!")
                .result(authResponse)
                .build();
    }

    /**
     * Đổi mật khẩu
     */
    @PutMapping("/change-password")
    @Operation(summary = "Đổi mật khẩu (đã đăng nhập)")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        authService.changePassword(authentication.getName(), request);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đổi mật khẩu thành công")
                .build();
    }

    /**
     * Gửi mã OTP để đặt lại mật khẩu
     */
    @PostMapping("/send-otp")
    @Operation(summary = "Gửi OTP quên mật khẩu")
    public ApiResponse<Void> sendOtp(@RequestBody EmailRequest request) {
        // 1. Kiểm tra email tồn tại
        if (!userService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống");
        }

        // 2. Sinh mã OTP và lưu vào Redis
        String otp = otpService.generateOtp(request.getEmail());

        // 3. TODO: Gửi email via RabbitMQ (Sẽ thực hiện ở bước sau)
        log.info("Mã OTP cho {} là: {}", request.getEmail(), otp);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Mã OTP đã được gửi về email (Debug: " + otp + ")")
                .build();
    }

    /**
     * Xác thực OTP và đặt lại mật khẩu mới
     */
    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực OTP và đặt mật khẩu mới")
    public ApiResponse<Void> verifyOtp(@RequestBody ResetPasswordRequest request) {
        // 1. Xác thực OTP từ Redis
        if (!otpService.verifyOtp(request.getEmail(), request.getOtp())) {
            throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        // 2. Thực hiện đổi mật khẩu
        authService.resetPassword(request.getEmail(), request.getPasswordNew());

        // 3. Xóa OTP sau khi dùng thành công
        otpService.deleteOtp(request.getEmail());

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Đổi mật khẩu thành công")
                .build();
    }

    /**
     * Gửi email xác thực tài khoản
     */
    @PostMapping("/verify-email")
    @Operation(summary = "Gửi email xác thực tài khoản")
    public ApiResponse<Void> verifyEmail(@RequestBody EmailRequest request) {
        String token = authService.generateEmailVerificationToken(request.getEmail());
        // TODO: Send verification email via Notification Service (RabbitMQ)
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Đã gửi email xác thực. Vui lòng kiểm tra hộp thư.")
                .build();
    }

    /**
     * Xác nhận email thông qua token
     */
    @GetMapping("/confirm-email")
    @Operation(summary = "Xác nhận email qua token (query)")
    public ApiResponse<Void> confirmEmail(@RequestParam("token") String token) {
        String email = authService.validateEmailVerificationToken(token);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Xác thực email thành công cho: " + email)
                .build();
    }

    /**
     * Làm mới access token bằng refresh token
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới access token")
    public ApiResponse<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse authResponse = authService.refreshToken(request.getRefreshToken());
        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Làm mới token thành công")
                .result(authResponse)
                .build();
    }


}
