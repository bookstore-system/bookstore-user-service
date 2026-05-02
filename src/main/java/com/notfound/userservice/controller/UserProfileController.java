package com.notfound.userservice.controller;

import com.notfound.userservice.model.dto.request.UpdateProfileRequest;
import com.notfound.userservice.model.dto.response.AddressDetailResponse;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.ContactInfoResponse;
import com.notfound.userservice.model.dto.response.UserBasicInfoResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.mapper.UserMapper;
import com.notfound.userservice.service.AddressService;
import com.notfound.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserProfileController {

    UserService userService;
    AddressService addressService;

    /**
     * Lấy thông tin user đang đăng nhập
     * GET /api/v1/users/profile
     */
    @GetMapping("/profile")
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
     * PUT /api/v1/users/profile
     * Consumes: multipart/form-data
     */
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserResponse> updateProfile(
            @ModelAttribute @Valid UpdateProfileRequest request,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("PUT /api/v1/users/profile - Updating profile for user: {}", currentUsername);

        UserResponse userResponse = userService.updateProfile(currentUsername, request);

        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Cập nhật thông tin thành công")
                .result(userResponse)
                .build();
    }

    /**
     * Lấy thông tin liên lạc của user (dùng cho Notification Service)
     * GET /api/v1/users/{userId}/contact-info
     */
    @GetMapping("/{userId}/contact-info")
    public ApiResponse<ContactInfoResponse> getContactInfo(@PathVariable UUID userId) {
        log.info("GET /api/v1/users/{}/contact-info - Getting contact info", userId);

        ContactInfoResponse contactInfo = userService.getUserContactInfo(userId);

        return ApiResponse.<ContactInfoResponse>builder()
                .code(200)
                .message("Lấy thông tin liên lạc thành công")
                .result(contactInfo)
                .build();
    }

    /**
     * Lấy chi tiết địa chỉ giao hàng của một user (dùng cho Order Service)
     * GET /api/v1/users/{userId}/addresses/{addressId}
     */
    @GetMapping("/{userId}/addresses/{addressId}")
    public ApiResponse<AddressDetailResponse> getAddressDetail(
            @PathVariable UUID userId,
            @PathVariable UUID addressId) {
        log.info("GET /api/v1/users/{}/addresses/{} - Getting address detail", userId, addressId);

        AddressDetailResponse addressDetail = addressService.getAddressDetail(userId, addressId);

        return ApiResponse.<AddressDetailResponse>builder()
                .code(1000)
                .message("Lấy chi tiết địa chỉ thành công")
                .result(addressDetail)
                .build();
    }
    // * Lấy thông tin hiển thị cơ bản của user (dùng cho Review Service)
    // GET /api/v1/users/{userId}/basic-info
    //
    @GetMapping("/{userId}/basic-info")
    public ApiResponse<UserBasicInfoResponse> getUserBasicInfo(@PathVariable UUID userId) {
        log.info("GET /api/v1/users/{}/basic-info - Getting basic info", userId);

        UserBasicInfoResponse basicInfo = userService.getUserBasicInfo(userId);

        return ApiResponse.<UserBasicInfoResponse>builder()
                .code(200)
                .message("Lấy thông tin cơ bản của user thành công")
                .result(basicInfo)
                .build();
    }
}
