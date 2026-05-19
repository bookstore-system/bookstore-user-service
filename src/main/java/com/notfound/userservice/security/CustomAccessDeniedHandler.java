package com.notfound.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Xử lý lỗi 403 Forbidden — khi user đã đăng nhập nhưng không đủ quyền.
 * Trả về JSON rõ ràng thay vì trang trắng mặc định.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        String json = """
                {
                  "timestamp": "%s",
                  "status": 403,
                  "error": "Forbidden",
                  "message": "Bạn không có quyền truy cập tài nguyên này. Endpoint: %s %s",
                  "details": [
                    "Nguyên nhân: %s",
                    "Hãy kiểm tra: (1) Token có hợp lệ không? (2) Tài khoản có đủ quyền (role) không?"
                  ]
                }
                """.formatted(Instant.now(), method, requestURI, accessDeniedException.getMessage());

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
