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
import com.notfound.userservice.messaging.EmailVerificationPublisher;
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
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
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

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    AuthService authService;
    UserService userService;
    OtpService otpService;
    PasswordResetOtpPublisher passwordResetOtpPublisher;
    EmailVerificationPublisher emailVerificationPublisher;
    GoogleOAuthProperties googleOAuthProperties;
    ObjectMapper objectMapper;

    @NonFinal
    @Value("${app.jwt.expiration-ms:86400000}")
    long accessTokenExpirationMs;

    @NonFinal
    @Value("${app.jwt.refresh-expiration-ms:604800000}")
    long refreshTokenExpirationMs;

    @NonFinal
    @Value("${app.auth.cookie.secure:false}")
    boolean secureCookies;

    @NonFinal
    @Value("${app.auth.cookie.same-site:Lax}")
    String cookieSameSite;

    @NonFinal
    @Value("${app.auth.cookie.domain:}")
    String cookieDomain;

    /**
     * Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    @Operation(summary = "Đăng ký tài khoản")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse authResponse = authService.register(request);
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Đăng ký thành công")
                .result(authResponse)
                .build();
        return withAuthCookies(response, authResponse);
    }

    /**
     * Đăng nhập vào hệ thống
     */
    @PostMapping("/login")
    @Operation(summary = "Đăng nhập")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);

        log.info("Đăng nhập thành công cho user: {}");

        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Đăng nhập thành công!")
                .result(authResponse)
                .build();
        return withAuthCookies(response, authResponse);
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
        emailVerificationPublisher.publish(request.getEmail(), token);
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
    public ResponseEntity<Void> confirmEmail(@RequestParam("token") String token) {
        try {
            String email = authService.validateEmailVerificationToken(token);
            return redirectToFrontend("email_verified=true&email=" + urlEncode(email));
        } catch (Exception e) {
            log.warn("Email verification failed", e);
            return redirectToFrontend("error=email_verification_failed");
        }
    }

    /**
     * Làm mới access token bằng refresh token
     */
    @PostMapping("/refresh-token")
    @Operation(summary = "Làm mới access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody(required = false) RefreshTokenRequest request,
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshTokenCookie) {
        String refreshToken = request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()
                ? request.getRefreshToken()
                : refreshTokenCookie;
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token không được để trống");
        }

        AuthResponse authResponse = authService.refreshToken(refreshToken);
        ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
                .code(1000)
                .message("Làm mới token thành công")
                .result(authResponse)
                .build();
        return withAuthCookies(response, authResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Đăng xuất")
    public ResponseEntity<ApiResponse<Void>> logout() {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(1000)
                .message("Đăng xuất thành công")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie(ACCESS_TOKEN_COOKIE).toString())
                .header(HttpHeaders.SET_COOKIE, expiredCookie(REFRESH_TOKEN_COOKIE).toString())
                .body(response);
    }

    @GetMapping("/google/callback")
    @Operation(summary = "Google OAuth callback")
    public ResponseEntity<Void> googleCallback(@RequestParam(value = "code", required = false) String code) {
        if (code == null || code.isBlank()) {
            return redirectToFrontend("error=google_invalid_code");
        }

        try {
            AuthResponse authResponse = authService.handleGoogleOAuthCallback(code);
            StringBuilder query = new StringBuilder("login=google");
            if (authResponse.getUser() != null) {
                query.append("&user=").append(urlEncode(objectMapper.writeValueAsString(authResponse.getUser())));
            }

            return redirectToFrontend(query.toString(), authResponse);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Google user response", e);
            return redirectToFrontend("error=google_login_failed");
        } catch (Exception e) {
            log.error("Google OAuth callback failed", e);
            return redirectToFrontend("error=google_login_failed");
        }
    }

    private ResponseEntity<Void> redirectToFrontend(String query) {
        return redirectToFrontend(query, null);
    }

    private ResponseEntity<Void> redirectToFrontend(String query, AuthResponse authResponse) {
        String baseUrl = googleOAuthProperties.getFrontendRedirectUrl();
        String separator = baseUrl.contains("?") ? "&" : "?";
        URI location = URI.create(baseUrl + separator + query);
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, location.toString());
        if (authResponse != null) {
            addAuthCookieHeaders(builder, authResponse);
        }
        return builder.build();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private ResponseEntity<ApiResponse<AuthResponse>> withAuthCookies(
            ApiResponse<AuthResponse> response,
            AuthResponse authResponse) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
        addAuthCookieHeaders(builder, authResponse);
        return builder.body(response);
    }

    private void addAuthCookieHeaders(ResponseEntity.BodyBuilder builder, AuthResponse authResponse) {
        if (authResponse.getToken() != null && !authResponse.getToken().isBlank()) {
            builder.header(HttpHeaders.SET_COOKIE, authCookie(
                    ACCESS_TOKEN_COOKIE,
                    authResponse.getToken(),
                    Duration.ofMillis(accessTokenExpirationMs)).toString());
        }
        if (authResponse.getRefreshToken() != null && !authResponse.getRefreshToken().isBlank()) {
            builder.header(HttpHeaders.SET_COOKIE, authCookie(
                    REFRESH_TOKEN_COOKIE,
                    authResponse.getRefreshToken(),
                    Duration.ofMillis(refreshTokenExpirationMs)).toString());
        }
    }

    private ResponseCookie authCookie(String name, String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(maxAge);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
    }

    private ResponseCookie expiredCookie(String name) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite(cookieSameSite)
                .path("/")
                .maxAge(Duration.ZERO);
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            builder.domain(cookieDomain);
        }
        return builder.build();
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
