package com.notfound.userservice.controller;

import com.notfound.userservice.model.dto.request.CreateUserRequest;
import com.notfound.userservice.model.dto.request.UpdateUserRequest;
import com.notfound.userservice.model.dto.request.UpdateUserStatusRequest;
import com.notfound.userservice.model.dto.request.UserFilterRequest;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.UserDetailResponse;
import com.notfound.userservice.model.dto.response.UserManagementResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.dto.response.UserStatsResponse;
import com.notfound.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Chỉ ADMIN mới truy cập được
public class UserController {

    UserService userService;

    /**
     * Lấy danh sách tất cả users với phân trang, tìm kiếm và lọc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<UserManagementResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("GET /api/admin/users - Getting all users");

        UserFilterRequest filterRequest = UserFilterRequest.builder()
                .search(search)
                .role(role)
                .status(status)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .page(page)
                .size(size)
                .build();

        Page<UserManagementResponse> users = userService.getAllUsers(filterRequest);

        return ResponseEntity.ok(ApiResponse.<Page<UserManagementResponse>>builder()
                .code(200)
                .message("Users retrieved successfully")
                .result(users)
                .build());
    }

    /**
     * Lấy chi tiết một user theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable UUID id) {
        log.info("GET /api/admin/users/{} - Getting user detail", id);

        UserDetailResponse user = userService.getUserById(id);

        return ResponseEntity.ok(ApiResponse.<UserDetailResponse>builder()
                .code(200)
                .message("User detail retrieved successfully")
                .result(user)
                .build());
    }

    /**
     * Tạo user mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody CreateUserRequest request
    ) {
        log.info("POST /api/admin/users - Creating new user: {}", request.getUsername());

        UserResponse user = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<UserResponse>builder()
                        .code(201)
                        .message("User created successfully")
                        .result(user)
                        .build());
    }

    /**
     * Cập nhật thông tin user
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        log.info("PUT /api/admin/users/{} - Updating user", id);

        UserResponse user = userService.updateUser(id, request);

        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .code(200)
                .message("User updated successfully")
                .result(user)
                .build());
    }

    /**
     * Xóa user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable UUID id) {
        log.info("DELETE /api/admin/users/{} - Deleting user", id);

        userService.deleteUser(id);

        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(200)
                .message("User deleted successfully")
                .build());
    }

    /**
     * Cập nhật trạng thái user (active/inactive/banned)
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserStatusRequest request
    ) {
        log.info("PATCH /api/admin/users/{}/status - Updating status to: {}", id, request.getStatus());

        UserResponse user = userService.updateUserStatus(id, request.getStatus());

        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .code(200)
                .message("User status updated successfully")
                .result(user)
                .build());
    }

    /**
     * Cấm user (ban)
     */
    @PatchMapping("/{id}/ban")
    public ResponseEntity<ApiResponse<UserResponse>> banUser(@PathVariable UUID id) {
        log.info("PATCH /api/admin/users/{}/ban - Banning user", id);

        UserResponse user = userService.banUser(id);

        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .code(200)
                .message("User banned successfully")
                .result(user)
                .build());
    }

    /**
     * Bỏ cấm user (unban)
     */
    @PatchMapping("/{id}/unban")
    public ResponseEntity<ApiResponse<UserResponse>> unbanUser(@PathVariable UUID id) {
        log.info("PATCH /api/admin/users/{}/unban - Unbanning user", id);

        UserResponse user = userService.unbanUser(id);

        return ResponseEntity.ok(ApiResponse.<UserResponse>builder()
                .code(200)
                .message("User unbanned successfully")
                .result(user)
                .build());
    }

    /**
     * Lấy thống kê tổng quan về users
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getStatistics(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        log.info("GET /api/admin/users/statistics - Getting user statistics");

        UserStatsResponse stats;
        if (startDate != null && endDate != null) {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            stats = userService.getUserStatistics(start, end);
        } else {
            stats = userService.getUserStatistics();
        }

        return ResponseEntity.ok(ApiResponse.<UserStatsResponse>builder()
                .code(200)
                .message("User statistics retrieved successfully")
                .result(stats)
                .build());
    }

    /**
     * Lấy danh sách top spenders
     */
    @GetMapping("/top-spenders")
    public ResponseEntity<ApiResponse<List<UserStatsResponse.TopUserResponse>>> getTopSpenders(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("GET /api/admin/users/top-spenders - Getting top {} spenders", limit);

        List<UserStatsResponse.TopUserResponse> topSpenders = userService.getTopSpenders(limit);

        return ResponseEntity.ok(ApiResponse.<List<UserStatsResponse.TopUserResponse>>builder()
                .code(200)
                .message("Top spenders retrieved successfully")
                .result(topSpenders)
                .build());
    }

    /**
     * Lấy danh sách top buyers (mua nhiều đơn nhất)
     */
    @GetMapping("/top-buyers")
    public ResponseEntity<ApiResponse<List<UserStatsResponse.TopUserResponse>>> getTopBuyers(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("GET /api/admin/users/top-buyers - Getting top {} buyers", limit);

        List<UserStatsResponse.TopUserResponse> topBuyers = userService.getTopBuyers(limit);

        return ResponseEntity.ok(ApiResponse.<List<UserStatsResponse.TopUserResponse>>builder()
                .code(200)
                .message("Top buyers retrieved successfully")
                .result(topBuyers)
                .build());
    }

    /**
     * Lấy danh sách users mới
     */
    @GetMapping("/new")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getNewUsers(
            @RequestParam(defaultValue = "7") int days
    ) {
        log.info("GET /api/admin/users/new - Getting new users in last {} days", days);

        List<UserResponse> newUsers = userService.getNewUsers(days);

        return ResponseEntity.ok(ApiResponse.<List<UserResponse>>builder()
                .code(200)
                .message("New users retrieved successfully")
                .result(newUsers)
                .build());
    }

    /**
     * Xuất dữ liệu users ra file Excel
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportUsers() {
        log.info("GET /api/admin/users/export - Exporting users to Excel");

        byte[] excelData = userService.exportUsersToExcel();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "users_export.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelData);
    }
}
