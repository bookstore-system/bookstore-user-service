package com.notfound.userservice.service.impl;

import com.notfound.userservice.model.dto.request.CreateUserRequest;
import com.notfound.userservice.model.dto.response.ContactInfoResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.enums.Role;
import com.notfound.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        mockUser = User.builder()
                .id(userId)
                .username("admin")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .status("active")
                .build();
    }

    @Test
    void createUser_Success() {
        // Arrange
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setRole("CUSTOMER");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        
        User savedUser = User.builder()
                .id(UUID.randomUUID())
                .username("newuser")
                .email("newuser@example.com")
                .role(Role.CUSTOMER)
                .status("active")
                .build();
                
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        UserResponse response = userService.createUser(request);

        // Assert
        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("CUSTOMER", response.getRole());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void banUser_Success() {
        // Arrange
        User targetUser = User.builder()
                .id(UUID.randomUUID())
                .username("baduser")
                .role(Role.CUSTOMER)
                .status("active")
                .build();

        when(userRepository.findById(targetUser.getId())).thenReturn(Optional.of(targetUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        UserResponse response = userService.banUser(targetUser.getId());

        // Assert
        assertNotNull(response);
        assertEquals("banned", response.getStatus());
    }

    @Test
    void banUser_ThrowsException_WhenTargetIsAdmin() {
        // Arrange
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser)); // mockUser is ADMIN

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.banUser(userId);
        });
        
        assertEquals("Không thể ban ADMIN", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void getUserContactInfo_Success() {
        // Arrange
        User user = User.builder()
                .id(userId)
                .username("testuser")
                .email("testuser@example.com")
                .phoneNumber("+84987654321")
                .role(Role.CUSTOMER)
                .status("active")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Act
        ContactInfoResponse response = userService.getUserContactInfo(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("testuser@example.com", response.getEmail());
        assertEquals("+84987654321", response.getPhoneNumber());
        assertNull(response.getDeviceToken());
    }

    @Test
    void getUserContactInfo_ThrowsException_WhenUserNotFound() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(userRepository.findById(unknownId)).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserContactInfo(unknownId);
        });

        assertEquals("Người dùng không tồn tại", exception.getMessage());
    }
}
