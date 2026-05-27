package com.notfound.userservice.service.impl;

import com.notfound.userservice.exception.EmailAlreadyInUseException;
import com.notfound.userservice.config.GoogleOAuthProperties;
import com.notfound.userservice.model.dto.request.ChangePasswordRequest;
import com.notfound.userservice.model.dto.request.GoogleAuthRequest;
import com.notfound.userservice.model.dto.request.LoginRequest;
import com.notfound.userservice.model.dto.request.RegisterRequest;
import com.notfound.userservice.model.dto.response.AuthResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.enums.AuthProvider;
import com.notfound.userservice.model.enums.Role;
import com.notfound.userservice.model.mapper.UserMapper;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.security.JwtService;
import com.notfound.userservice.service.AuthService;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RestTemplate restTemplate;
    private final GoogleOAuthProperties googleOAuthProperties;

    public AuthServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            UserMapper userMapper,
            RestTemplate restTemplate,
            GoogleOAuthProperties googleOAuthProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.restTemplate = restTemplate;
        this.googleOAuthProperties = googleOAuthProperties;
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
    @Transactional
    public AuthResponse loginWithGoogle(GoogleAuthRequest request) {
        if (!googleOAuthProperties.hasClientId()) {
            throw new IllegalArgumentException("Google OAuth client id is not configured");
        }

        Map<String, Object> googleUserInfo = verifyGoogleCredential(request.getCredential());
        return buildGoogleAuthResponse(googleUserInfo);
    }

    @Override
    @Transactional
    public AuthResponse handleGoogleOAuthCallback(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Google authorization code is required");
        }
        if (!googleOAuthProperties.hasAuthorizationCodeConfig()) {
            throw new IllegalArgumentException("Google OAuth authorization code flow is not configured");
        }

        String accessToken = exchangeCodeForAccessToken(code);
        Map<String, Object> googleUserInfo = fetchGoogleUserInfo(accessToken);
        return buildGoogleAuthResponse(googleUserInfo);
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
        String normalizedEmail = email.trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại trong hệ thống"));
        return jwtService.generateToken(normalizedEmail);
    }

    private Map<String, Object> verifyGoogleCredential(String credential) {
        String tokenInfoUrl = "https://oauth2.googleapis.com/tokeninfo?id_token=" + credential;
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                tokenInfoUrl,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalArgumentException("Google credential is invalid");
        }

        String audience = stringValue(body.get("aud"));
        if (!googleOAuthProperties.getClientId().equals(audience)) {
            throw new IllegalArgumentException("Google credential audience is invalid");
        }

        if (!isGoogleEmailVerified(body.get("email_verified"))) {
            throw new IllegalArgumentException("Google email is not verified");
        }

        return body;
    }

    private String exchangeCodeForAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", googleOAuthProperties.getClientId());
        params.add("client_secret", googleOAuthProperties.getClientSecret());
        params.add("redirect_uri", googleOAuthProperties.getRedirectUri());
        params.add("grant_type", "authorization_code");

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://oauth2.googleapis.com/token",
                HttpMethod.POST,
                new HttpEntity<>(params, headers),
                new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        String accessToken = body != null ? stringValue(body.get("access_token")) : null;
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("Google authorization code is invalid");
        }
        return accessToken;
    }

    private Map<String, Object> fetchGoogleUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://www.googleapis.com/oauth2/v2/userinfo",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});

        Map<String, Object> body = response.getBody();
        if (body == null || stringValue(body.get("email")) == null) {
            throw new IllegalArgumentException("Google user info is invalid");
        }
        return body;
    }

    private AuthResponse buildGoogleAuthResponse(Map<String, Object> googleUserInfo) {
        User user = createOrUpdateUserFromGoogle(googleUserInfo);
        user.setLastLogin(LocalDateTime.now());
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getUsername(), Map.of(
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

    private User createOrUpdateUserFromGoogle(Map<String, Object> googleUserInfo) {
        String email = stringValue(googleUserInfo.get("email"));
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Google account does not provide an email");
        }

        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String name = stringValue(googleUserInfo.get("name"));
        String picture = stringValue(googleUserInfo.get("picture"));
        String providerId = stringValue(googleUserInfo.get("sub"));
        if (providerId == null) {
            providerId = stringValue(googleUserInfo.get("id"));
        }

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (user == null) {
            user = User.builder()
                    .username(generateUniqueUsername(normalizedEmail))
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .email(normalizedEmail)
                    .fullName(name)
                    .avatar_url(picture)
                    .role(Role.CUSTOMER)
                    .status("active")
                    .authProvider(AuthProvider.GOOGLE)
                    .providerId(providerId)
                    .isEmailVerified(true)
                    .build();
            return userRepository.save(user);
        }

        if (name != null && !name.isBlank()) {
            user.setFullName(name);
        }
        if (picture != null && !picture.isBlank()) {
            user.setAvatar_url(picture);
        }
        user.setAuthProvider(AuthProvider.GOOGLE);
        user.setProviderId(providerId);
        user.setIsEmailVerified(true);
        return user;
    }

    private String generateUniqueUsername(String email) {
        String base = email.substring(0, email.indexOf("@"))
                .replaceAll("[^a-zA-Z0-9_]", "_");
        if (base.isBlank()) {
            base = "google_user";
        }

        String username = base;
        int suffix = 1;
        while (userRepository.existsByUsername(username)) {
            username = base + "_" + suffix;
            suffix++;
        }
        return username;
    }

    private static boolean isGoogleEmailVerified(Object value) {
        if (value instanceof Boolean verified) {
            return verified;
        }
        return value != null && "true".equalsIgnoreCase(value.toString());
    }

    private static String stringValue(Object value) {
        return value != null ? value.toString() : null;
    }

    @Override
    @Transactional
    public String validateEmailVerificationToken(String token) {
        String email = jwtService.extractSubject(token);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new IllegalArgumentException("Email xác thực không tồn tại trong hệ thống"));

        user.setIsEmailVerified(true);
        userRepository.save(user);

        return user.getEmail();
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
