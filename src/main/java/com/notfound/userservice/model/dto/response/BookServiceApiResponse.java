package com.notfound.userservice.model.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Envelope JSON từ bookstore-book-service ({@code ApiResponse} với field {@code data}).
 * Dùng cho OpenFeign deserialize đúng khi gọi batch books.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookServiceApiResponse<T> {
    int code;
    String message;
    T data;
}
