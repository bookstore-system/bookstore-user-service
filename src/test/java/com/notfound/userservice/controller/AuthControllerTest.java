package com.notfound.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.notfound.userservice.exception.GlobalExceptionHandler;
import com.notfound.userservice.model.dto.request.*;
import com.notfound.userservice.model.dto.response.AuthResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.service.AuthService;
import com.notfound.userservice.service.OtpService;
import com.notfound.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private MockMvc mockMvc;
    private AuthService authService;
    private UserService userService;
    private OtpService otpService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        userService = mock(UserService.class);
        otpService = mock(OtpService.class);
        objectMapper = new ObjectMapper().findAndRegisterModules();

        AuthController controller = new AuthController(authService, userService, otpService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                        .build();
    }

    @Test
    void register_returnsAuthResponse() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("user1");
        request.setEmail("u@example.com");
        request.setPassword("password123");

        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(AuthResponse.builder().token("t").refreshToken("rt").build());

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đăng ký thành công"))
                .andExpect(jsonPath("$.result.token").value("t"));
    }

    @Test
    void login_returnsAuthResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("u");
        request.setPassword("p");

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(AuthResponse.builder().token("t").refreshToken("rt").build());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.message").value("Đăng nhập thành công!"))
                .andExpect(jsonPath("$.result.refreshToken").value("rt"));
    }

    @Test
    void changePassword_callsService() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setOldPassword("old");
        request.setNewPassword("newPassword123");
        request.setConfimPassword("newPassword123");

        mockMvc.perform(put("/api/v1/auth/change-password")
                        .principal(new UsernamePasswordAuthenticationToken("u", "N/A"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        verify(authService).changePassword(eq("u"), any(ChangePasswordRequest.class));
    }

    @Test
    void sendOtp_whenEmailNotExists_returns400() throws Exception {
        when(userService.existsByEmail("nope@example.com")).thenReturn(false);

        EmailRequest request = new EmailRequest();
        request.setEmail("nope@example.com");

        mockMvc.perform(post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void sendOtp_whenEmailExists_returns200AndCallsOtpService() throws Exception {
        when(userService.existsByEmail("u@example.com")).thenReturn(true);
        when(otpService.generateOtp("u@example.com")).thenReturn("123456");

        EmailRequest request = new EmailRequest();
        request.setEmail("u@example.com");

        mockMvc.perform(post("/api/v1/auth/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("123456")));

        verify(otpService).generateOtp("u@example.com");
    }

    @Test
    void verifyOtp_whenInvalid_returns400() throws Exception {
        when(otpService.verifyOtp("u@example.com", "000000")).thenReturn(false);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("u@example.com");
        request.setOtp("000000");
        request.setPasswordNew("newPassword123");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void verifyOtp_whenValid_callsResetAndDelete() throws Exception {
        when(otpService.verifyOtp("u@example.com", "123456")).thenReturn(true);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("u@example.com");
        request.setOtp("123456");
        request.setPasswordNew("newPassword123");

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công"));

        verify(authService).resetPassword("u@example.com", "newPassword123");
        verify(otpService).deleteOtp("u@example.com");
    }

    @Test
    void refreshToken_returnsAuthResponse() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("rt");
        when(authService.refreshToken("rt")).thenReturn(AuthResponse.builder().token("t2").build());

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.token").value("t2"));
    }

    @Test
    void googleCallback_returns501() throws Exception {
        mockMvc.perform(get("/api/v1/auth/google/callback").param("code", "abc"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.code").value(501));
    }

    @Test
    void me_returnsUserResponse() throws Exception {
        when(userService.getUserByUsername("u"))
                .thenReturn(UserResponse.builder().username("u").build());

        mockMvc.perform(get("/api/v1/auth/me")
                        .principal(new UsernamePasswordAuthenticationToken("u", "N/A")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.username").value("u"));
    }
}

