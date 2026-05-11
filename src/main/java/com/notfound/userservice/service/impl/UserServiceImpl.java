package com.notfound.userservice.service.impl;

import com.notfound.userservice.client.OrderClient;
import com.notfound.userservice.client.ReviewClient;
import com.notfound.userservice.exception.ResourceNotFoundException;
import com.notfound.userservice.model.dto.request.CreateUserRequest;
import com.notfound.userservice.model.dto.request.UpdateProfileRequest;
import com.notfound.userservice.model.dto.request.UpdateUserRequest;
import com.notfound.userservice.model.dto.request.UserFilterRequest;
import com.notfound.userservice.model.dto.response.ContactInfoResponse;
import com.notfound.userservice.model.dto.response.UserBasicInfoResponse;
import com.notfound.userservice.model.dto.response.UserDetailResponse;
import com.notfound.userservice.model.dto.response.UserManagementResponse;
import com.notfound.userservice.model.dto.response.UserOrderSummaryResponse;
import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.dto.response.UserReviewCountResponse;
import com.notfound.userservice.model.dto.response.UserStatsResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.enums.Role;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserServiceImpl implements UserService {

    PasswordEncoder passwordEncoder;
    UserRepository userRepository;
    OrderClient orderClient;
    ReviewClient reviewClient;

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ===== CRUD OPERATIONS =====

    @Override
    @Transactional(readOnly = true)
    public Page<UserManagementResponse> getAllUsers(UserFilterRequest filterRequest) {
        log.info("Getting all users with filters: {}", filterRequest);

        int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
        int size = filterRequest.getSize() != null ? filterRequest.getSize() : 10;
        String sortBy = filterRequest.getSortBy() != null ? filterRequest.getSortBy() : "username";
        String sortDirection = filterRequest.getSortDirection() != null ? filterRequest.getSortDirection() : "asc";

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Role role = null;
        if (filterRequest.getRole() != null && !filterRequest.getRole().isEmpty()) {
            try {
                role = Role.valueOf(filterRequest.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role: {}", filterRequest.getRole());
            }
        }

        Page<User> userPage = userRepository.findByFilters(
                filterRequest.getSearch(),
                role,
                filterRequest.getStatus(),
                pageable);

        return userPage.map(this::mapToUserManagementResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailResponse getUserById(UUID id) {
        log.info("Getting user detail by id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        return mapToUserDetailResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.info("Getting user detail by username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        return mapToUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username đã tồn tại");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email đã tồn tại");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .avatar_url(request.getAvatarUrl())
                .dateOfBirth(request.getDateOfBirth())
                .role(request.getRole() != null ? Role.valueOf(request.getRole()) : Role.CUSTOMER)
                .status("active")
                .build();

        User savedUser = userRepository.save(user);
        log.info("Created user successfully: {}", savedUser.getId());

        return mapToUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(UUID id, UpdateUserRequest request) {
        log.info("Updating user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new IllegalArgumentException("Email đã tồn tại");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        User updatedUser = userRepository.save(user);
        return mapToUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Không thể xóa ADMIN");
        }

        userRepository.delete(user);
    }

    // ===== STATUS MANAGEMENT =====

    @Override
    @Transactional
    public UserResponse updateUserStatus(UUID id, String status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (!status.matches("^(active|inactive|banned)$")) {
            throw new IllegalArgumentException("Trạng thái không hợp lệ: " + status);
        }

        user.setStatus(status);
        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse banUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (user.getRole() == Role.ADMIN) {
            throw new IllegalArgumentException("Không thể ban ADMIN");
        }

        user.setStatus("banned");
        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse unbanUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        user.setStatus("active");
        return mapToUserResponse(userRepository.save(user));
    }

    // ===== STATISTICS =====

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStatistics() {
        return getUserStatistics(null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByStatus("active");
        long inactiveUsers = userRepository.countByStatus("inactive");
        long bannedUsers = userRepository.countByStatus("banned");

        long totalAdmins = userRepository.countByRole(Role.ADMIN);
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        long totalGuests = userRepository.countByRole(Role.GUEST);

        String startStr = startDate != null ? startDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        String endStr = endDate != null ? endDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        
        UserStatsResponse orderStats = null;
        try {
            orderStats = orderClient.getOrderStats(startStr, endStr);
            log.info("Fetched order stats successfully: {}", orderStats);
        } catch (Exception e) {
            log.warn("Failed to fetch order stats from order-service", e);
        }

        BigDecimal totalRevenue = orderStats != null && orderStats.getTotalRevenue() != null ? orderStats.getTotalRevenue() : BigDecimal.ZERO;
        long totalOrders = orderStats != null && orderStats.getTotalOrders() != null ? orderStats.getTotalOrders() : 0L;
        BigDecimal avgRevenuePerUser = orderStats != null && orderStats.getAvgRevenuePerUser() != null ? orderStats.getAvgRevenuePerUser() : BigDecimal.ZERO;
        BigDecimal avgOrderValue = orderStats != null && orderStats.getAvgOrderValue() != null ? orderStats.getAvgOrderValue() : BigDecimal.ZERO;
        String currency = orderStats != null && orderStats.getCurrency() != null ? orderStats.getCurrency() : "VND";

        return UserStatsResponse.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .bannedUsers(bannedUsers)
                .totalAdmins(totalAdmins)
                .totalCustomers(totalCustomers)
                .totalGuests(totalGuests)
                .newUsersThisMonth(0L)
                .newUsersThisWeek(0L)
                .newUsersToday(0L)
                .totalRevenue(totalRevenue)
                .avgRevenuePerUser(avgRevenuePerUser)
                .avgOrderValue(avgOrderValue)
                .totalOrders(totalOrders)
                .currency(currency)
                .topSpenders(getTopSpenders(5))
                .topBuyers(getTopBuyers(5))   
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStatsResponse.TopUserResponse> getTopSpenders(int limit) {
        try {
            List<UserStatsResponse.TopUserResponse> spenders = orderClient.getTopSpenders(limit);
            return fetchAndHydrateTopUsers(spenders);
        } catch (Exception e) {
            log.warn("Failed to fetch top spenders from order-service", e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStatsResponse.TopUserResponse> getTopBuyers(int limit) {
        try {
            List<UserStatsResponse.TopUserResponse> buyers = orderClient.getTopBuyers(limit);
            return fetchAndHydrateTopUsers(buyers);
        } catch (Exception e) {
            log.warn("Failed to fetch top buyers from order-service", e);
            return List.of();
        }
    }

    private List<UserStatsResponse.TopUserResponse> fetchAndHydrateTopUsers(List<UserStatsResponse.TopUserResponse> topUsers) {
        if (topUsers == null || topUsers.isEmpty()) {
            return List.of();
        }

        List<UUID> userIds = topUsers.stream()
                .filter(u -> u.getUserId() != null)
                .map(u -> {
                    try {
                        return UUID.fromString(u.getUserId());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());

        List<User> users = userRepository.findAllById(userIds);
        var userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        for (var topUser : topUsers) {
            try {
                UUID id = UUID.fromString(topUser.getUserId());
                User user = userMap.get(id);
                if (user != null) {
                    topUser.setUsername(user.getUsername());
                    topUser.setEmail(user.getEmail());
                    topUser.setFullName(user.getFullName());
                    topUser.setAvatarUrl(user.getAvatar_url());
                }
            } catch (IllegalArgumentException ignored) {}
        }

        return topUsers;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getNewUsers(int days) {
        return List.of();
    }

    @Override
    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getDateOfBirth() != null) {
            user.setDateOfBirth(request.getDateOfBirth());
        }

        // Upload avatar TODO: Implement ImageService / Cloudinary integration
        
        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public ContactInfoResponse getUserContactInfo(UUID userId) {
        log.info("Getting contact info for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        return ContactInfoResponse.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .deviceToken(null) // Device token not yet supported
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UserBasicInfoResponse getUserBasicInfo(UUID userId) {
        log.info("Getting basic info for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại"));

        return UserBasicInfoResponse.builder()
                .userId(user.getId())
                .displayName(user.getFullName())
                .avatarUrl(user.getAvatar_url())
                .build();
    }

    @Override
    public byte[] exportUsersToExcel() {
        // TODO: Implement excel export (cần thêm Apache POI)
        throw new UnsupportedOperationException("Chưa hỗ trợ xuất Excel");
    }

    // ===== MAPPING METHODS =====

    private UserManagementResponse mapToUserManagementResponse(User user) {
        UserOrderSummaryResponse summary = getUserOrderSummarySafely(user.getId());
        
        return UserManagementResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .avatarUrl(user.getAvatar_url())
                .role(user.getRole().name())
                .status(user.getStatus() != null ? user.getStatus() : "active")
                .dateOfBirth(user.getDateOfBirth())
                .points(user.getPoints())
                .membershipTier(user.getMembershipTier() != null ? user.getMembershipTier().name() : null)
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .lastLogin(user.getLastLogin())
                .totalOrders(summary.getTotalOrders())
                .totalSpent(summary.getTotalSpent())
                .build();
    }

    private UserDetailResponse mapToUserDetailResponse(User user) {
        UserOrderSummaryResponse summary = getUserOrderSummarySafely(user.getId());
        UserReviewCountResponse reviewCount = getUserReviewCountSafely(user.getId());

        return UserDetailResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .avatarUrl(user.getAvatar_url())
                .role(user.getRole().name())
                .status(user.getStatus() != null ? user.getStatus() : "active")
                .dateOfBirth(user.getDateOfBirth())
                .points(user.getPoints())
                .membershipTier(user.getMembershipTier() != null ? user.getMembershipTier().name() : null)
                .isEmailVerified(user.getIsEmailVerified())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .providerId(user.getProviderId())
                .totalOrders(summary.getTotalOrders())
                .totalSpent(summary.getTotalSpent())
                .totalReviews(reviewCount.getTotalReviews())
                .lastOrderDate(summary.getLastOrderDate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLogin())
                .build();
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender())
                .role(user.getRole().name())
                .status(user.getStatus() != null ? user.getStatus() : "active")
                .dateOfBirth(user.getDateOfBirth())
                .points(user.getPoints())
                .membershipTier(user.getMembershipTier() != null ? user.getMembershipTier().name() : null)
                .isEmailVerified(user.getIsEmailVerified())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .lastLogin(user.getLastLogin())
                .avatar(user.getAvatar_url())
                .build();
    }

    private UserOrderSummaryResponse getUserOrderSummarySafely(UUID userId) {
        try {
            UserOrderSummaryResponse summary = orderClient.getUserOrderSummary(userId);
            if (summary != null) {
                return summary;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch order summary for user {}: {}", userId, e.getMessage());
        }
        return UserOrderSummaryResponse.builder()
                .userId(userId)
                .totalOrders(0)
                .totalSpent(BigDecimal.ZERO)
                .lastOrderDate(null)
                .build();
    }

    private UserReviewCountResponse getUserReviewCountSafely(UUID userId) {
        try {
            UserReviewCountResponse reviewCount = reviewClient.getUserReviewCount(userId);
            if (reviewCount != null) {
                return reviewCount;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch review count for user {}: {}", userId, e.getMessage());
        }
        return UserReviewCountResponse.builder()
                .userId(userId)
                .totalReviews(0)
                .build();
    }
}
