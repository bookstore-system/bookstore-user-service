package com.notfound.userservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Xử lý lỗi 401 Unauthorized — khi request không có token hoặc token không hợp lệ.
 * Trả về JSON rõ ràng thay vì trang trắng mặc định.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        String json = """
                {
                  "timestamp": "%s",
                  "status": 401,
                  "error": "Unauthorized",
                  "message": "Bạn chưa đăng nhập hoặc token đã hết hạn. Endpoint: %s %s",
                  "details": [
                    "Nguyên nhân: %s",
                    "Hãy gửi header: Authorization: Bearer <your_token>",
                    "Nếu chưa có token, hãy gọi POST /api/v1/auth/login để lấy token."
                  ]
                }
                """.formatted(Instant.now(), method, requestURI, authException.getMessage());

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }
}
