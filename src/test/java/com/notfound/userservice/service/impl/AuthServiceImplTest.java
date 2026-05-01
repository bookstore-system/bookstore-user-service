package com.notfound.userservice.service.impl;

import com.notfound.userservice.model.dto.request.LoginRequest;
import com.notfound.userservice.model.dto.request.RegisterRequest;
import com.notfound.userservice.model.dto.response.AuthResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.enums.Role;
import com.notfound.userservice.model.mapper.UserMapper;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.security.JwtService;
import com.notfound.userservice.exception.EmailAlreadyInUseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("encoded_password")
                .role(Role.CUSTOMER)
                .build();

        mockUserResponse = UserResponse.builder()
                .id(mockUser.getId())
                .username("testuser")
                .email("test@example.com")
                .role("CUSTOMER")
                .build();
    }

    @Test
    void register_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setFullName("Test User");
        request.setPhoneNumber("0123456789");
        request.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_password");
        
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(mockUser.getId());
            return saved;
        });
        
        when(jwtService.generateToken(eq(mockUser.getUsername()), any(Map.class))).thenReturn("mocked_jwt_token");
        when(jwtService.generateRefreshToken(mockUser.getUsername())).thenReturn("mocked_refresh_token");
        when(userMapper.toUserResponse(any(User.class))).thenReturn(mockUserResponse);

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("mocked_jwt_token", response.getToken());
        assertEquals("mocked_refresh_token", response.getRefreshToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_ThrowsException_WhenUsernameExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyInUseException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_Success() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        // mock authenticationManager.authenticate returning something or just not throwing
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(mockUser));
        when(jwtService.generateToken(eq(mockUser.getUsername()), any(Map.class))).thenReturn("mocked_jwt_token");
        when(jwtService.generateRefreshToken(mockUser.getUsername())).thenReturn("mocked_refresh_token");
        when(userMapper.toUserResponse(mockUser)).thenReturn(mockUserResponse);

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertNotNull(response);
        assertEquals("mocked_jwt_token", response.getToken());
        assertEquals("mocked_refresh_token", response.getRefreshToken());
        assertEquals("testuser", response.getUser().getUsername());
        verify(authenticationManager, times(1)).authenticate(any());
    }

    @Test
    void login_ThrowsException_WhenUserNotFound() {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsername("wronguser");
        request.setPassword("password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> authService.login(request));
    }
}
