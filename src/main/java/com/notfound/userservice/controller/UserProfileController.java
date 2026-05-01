package com.notfound.userservice.controller;

import com.notfound.userservice.model.dto.request.UpdateProfileRequest;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.mapper.UserMapper;
import com.notfound.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/profile")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserProfileController {

    UserService userService;

    /**
     * Lấy thông tin user đang đăng nhập
     * GET /api/user/me
     */
    @GetMapping
    public ApiResponse<UserResponse> getCurrentUser(Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("GET /api/v1/users/profile - Getting profile for user: {}", currentUsername);
        
        UserResponse userResponse = userService.getUserByUsername(currentUsername);

        return ApiResponse.<UserResponse>builder()
                .code(1000)
                .message("Lấy thông tin user thành công")
                .result(userResponse)
                .build();
    }

    /**
     * Cập nhật thông tin cá nhân của user đang đăng nhập
     * PUT /api/user/profile
     * Consumes: multipart/form-data
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateProfile(
            @ModelAttribute @Valid UpdateProfileRequest request,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("PUT /api/user/profile - Updating profile for user: {}", currentUsername);

        UserResponse userResponse = userService.updateProfile(currentUsername, request);

        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Cập nhật thông tin thành công")
                .result(userResponse)
                .build();
    }
}
