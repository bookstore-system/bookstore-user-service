package com.notfound.userservice.config;

import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.enums.Role;
import com.notfound.userservice.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Configuration
@Slf4j
public class DataInitializer {
    
    PasswordEncoder passwordEncoder;

    @Bean
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        return args -> {
            try {
                // Kiểm tra xem đã có tài khoản admin nào chưa
                if (!userRepository.existsByUsername("admin")) {
                    User adminUser = User.builder()
                            .email("admin@gmail.com")
                            .username("admin")
                            .fullName("System Admin")
                            .password(passwordEncoder.encode("admin"))
                            .role(Role.ADMIN)
                            .status("active")
                            .points(0)
                            .isEmailVerified(true)
                            .build();
                    userRepository.save(adminUser);
                    log.warn("=========================================================");
                    log.warn("Tài khoản ADMIN mặc định đã được tạo!");
                    log.warn("Username: admin");
                    log.warn("Password: admin");
                    log.warn("Vui lòng đổi mật khẩu sau khi đăng nhập lần đầu tiên.");
                    log.warn("=========================================================");
                }
            } catch (Exception e) {
                log.error("Lỗi khi khởi tạo tài khoản admin: {}", e.getMessage());
            }
        };
    }
}
