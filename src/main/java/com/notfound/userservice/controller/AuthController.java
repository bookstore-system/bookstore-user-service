package com.notfound.userservice.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.userservice.config.GoogleOAuthProperties;
import com.notfound.userservice.model.dto.request.*;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.AuthResponse;
import com.notfound.userservice.model.dto.response.IntrospectResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.mapper.UserMapper;
import com.notfound.userservice.messaging.PasswordResetOtpPublisher;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
    PasswordResetOtpPublisher passwordResetOtpPublisher;
    GoogleOAuthProperties googleOAuthProperties;
    ObjectMapper objectMapper;

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

        log.info("Đăng nhập thành công cho user: {}");

        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công!")
                .result(authResponse)
                .build();
    }

    /**
     * Đăng nhập bằng Google One Tap / Google Identity Services credential.
     */
    @PostMapping("/google")
    @Operation(summary = "Đăng nhập bằng Google credential")
    public ApiResponse<AuthResponse> loginWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse authResponse = authService.loginWithGoogle(request);

        return ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Đăng nhập Google thành công")
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

        // 3. Gửi event qua RabbitMQ để notification-service gửi email OTP
        passwordResetOtpPublisher.publish(request.getEmail(), otp);

        return ApiResponse.<Void>builder()
                .code(200)
                .message("Mã OTP đã được gửi về email")
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
        if (!userService.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email không tồn tại trong hệ thống");
        }

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

    @GetMapping("/google/callback")
    @Operation(summary = "Google OAuth callback")
    public ResponseEntity<Void> googleCallback(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.isBlank()) {
            return redirectToFrontend("error=google_invalid_code");
        }

        try {
            AuthResponse authResponse = authService.handleGoogleOAuthCallback(code);
            StringBuilder query = new StringBuilder()
                    .append("token=").append(urlEncode(authResponse.getToken()));

            if (authResponse.getRefreshToken() != null) {
                query.append("&refreshToken=").append(urlEncode(authResponse.getRefreshToken()));
            }
            if (authResponse.getUser() != null) {
                query.append("&user=").append(urlEncode(objectMapper.writeValueAsString(authResponse.getUser())));
            }

            return redirectToFrontend(query.toString());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Google user response", e);
            return redirectToFrontend("error=google_login_failed");
        } catch (Exception e) {
            log.error("Google OAuth callback failed", e);
            return redirectToFrontend("error=google_login_failed");
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String query) {
        String baseUrl = googleOAuthProperties.getFrontendRedirectUrl();
        String separator = baseUrl.contains("?") ? "&" : "?";
        URI location = URI.create(baseUrl + separator + query);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString())
                .build();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Alias endpoint để lấy thông tin user đang đăng nhập (tương thích FE).
     * Endpoint chuẩn bên user-service là GET /api/v1/users/profile
     */
    @GetMapping("/me")
    @Operation(summary = "Lấy user hiện tại (alias)")
    public ApiResponse<UserResponse> me(@Parameter(hidden = true) Authentication authentication) {
        String currentUsername = authentication.getName();
        UserResponse userResponse = userService.getUserByUsername(currentUsername);

        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Lấy thông tin user thành công")
                .result(userResponse)
                .build();
    }


}
