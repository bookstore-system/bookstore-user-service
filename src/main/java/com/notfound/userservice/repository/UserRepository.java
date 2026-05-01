package com.notfound.userservice.repository;

import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    // Đăng nhập
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // Kiểm tra trùng lặp
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    // Admin: Quản lý user
    Page<User> findByRole(Role role, Pageable pageable);

    // Thống kê theo role
    long countByRole(Role role);

    // Thống kê theo status
    long countByStatus(String status);

    // Lọc theo status
    Page<User> findByStatus(String status, Pageable pageable);

    // Lọc theo role và status
    Page<User> findByRoleAndStatus(Role role, String status, Pageable pageable);

    // Tìm kiếm và lọc phức tạp
    @Query("SELECT u FROM User u WHERE " +
           "(:search IS NULL OR LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:role IS NULL OR u.role = :role) AND " +
           "(:status IS NULL OR u.status = :status)")
    Page<User> findByFilters(@Param("search") String search,
                             @Param("role") Role role,
                             @Param("status") String status,
                             Pageable pageable);

    // Lấy UUID từ username
    @Query("SELECT u.id FROM User u WHERE u.username = :username")
    Optional<UUID> findIdByUsername(@Param("username") String username);
}
