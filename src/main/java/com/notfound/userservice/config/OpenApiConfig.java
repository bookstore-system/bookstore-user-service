package com.notfound.userservice.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI bookstoreUserServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bookstore User Service API")
                        .description("REST API: xác thực, hồ sơ người dùng, địa chỉ, wishlist, quản trị user (ADMIN). "
                                + "Endpoint cần JWT: mở Authorize, scheme Bearer, dán access token.")
                        .version("v1"))
                .addSecurityItem(new SecurityRequirement().addList(BEARER))
                .components(new Components().addSecuritySchemes(BEARER,
                        new SecurityScheme()
                                .name(BEARER)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Access token từ `/api/v1/auth/login` hoặc `/api/v1/auth/register`")));
    }
}
