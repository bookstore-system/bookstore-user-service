package com.notfound.userservice.controller;

import com.notfound.userservice.model.dto.request.AddBookToWishlistRequest;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.BookInWishlistResponse;
import com.notfound.userservice.model.dto.response.WishlistResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users/wishlist")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "User — Wishlist", description = "Wishlist của user đang đăng nhập")
public class WishlistController {

    WishlistService wishlistService;
    UserRepository userRepository;

    /**
     * Lấy userId thật từ Authentication (username trong JWT token).
     */
    private UUID resolveUserId(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại: " + username));
        return user.getId();
    }

    @GetMapping
    @Operation(summary = "Lấy wishlist của tôi")
    public ApiResponse<WishlistResponse> getMyWishlist(@Parameter(hidden = true) Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ApiResponse.<WishlistResponse>builder()
                .code(1000)
                .message("Lấy wishlist thành công")
                .result(wishlistService.getMyWishlist(userId))
                .build();
    }

    @PostMapping
    @Operation(summary = "Thêm sách vào wishlist")
    public ApiResponse<WishlistResponse> addBookToWishlist(
            @Valid @RequestBody AddBookToWishlistRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ApiResponse.<WishlistResponse>builder()
                .code(1000)
                .message("Thêm sách vào wishlist thành công")
                .result(wishlistService.addBookToWishlist(userId, request))
                .build();
    }

    @DeleteMapping("/{bookId}")
    @Operation(summary = "Xóa một sách khỏi wishlist")
    public ApiResponse<Void> removeBookFromWishlist(@PathVariable UUID bookId,
            @Parameter(hidden = true) Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        wishlistService.removeBookFromWishlist(userId, bookId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã xóa sách khỏi wishlist thành công")
                .build();
    }

    @DeleteMapping
    @Operation(summary = "Xóa toàn bộ wishlist")
    public ApiResponse<Void> clearWishlist(@Parameter(hidden = true) Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        wishlistService.clearWishlist(userId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã xóa toàn bộ wishlist thành công")
                .build();
    }

    @GetMapping("/check/{bookId}")
    @Operation(summary = "Kiểm tra sách có trong wishlist")
    public ApiResponse<BookInWishlistResponse> checkBookInWishlist(@PathVariable UUID bookId,
            @Parameter(hidden = true) Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        boolean isInWishlist = wishlistService.isBookInWishlist(userId, bookId);

        BookInWishlistResponse response = BookInWishlistResponse.builder()
                .bookId(bookId)
                .inWishlist(isInWishlist)
                .build();

        return ApiResponse.<BookInWishlistResponse>builder()
                .code(1000)
                .result(response)
                .build();
    }
}
