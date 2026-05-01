package com.notfound.userservice.controller;

import com.notfound.userservice.model.dto.request.AddBookToWishlistRequest;
import com.notfound.userservice.model.dto.response.ApiResponse;
import com.notfound.userservice.model.dto.response.BookInWishlistResponse;
import com.notfound.userservice.model.dto.response.WishlistResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.service.WishlistService;
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
    public ApiResponse<WishlistResponse> getMyWishlist(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ApiResponse.<WishlistResponse>builder()
                .code(1000)
                .message("Lấy wishlist thành công")
                .result(wishlistService.getMyWishlist(userId))
                .build();
    }

    @PostMapping
    public ApiResponse<WishlistResponse> addBookToWishlist(
            @Valid @RequestBody AddBookToWishlistRequest request,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        return ApiResponse.<WishlistResponse>builder()
                .code(1000)
                .message("Thêm sách vào wishlist thành công")
                .result(wishlistService.addBookToWishlist(userId, request))
                .build();
    }

    @DeleteMapping("/{bookId}")
    public ApiResponse<Void> removeBookFromWishlist(@PathVariable UUID bookId,
            Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        wishlistService.removeBookFromWishlist(userId, bookId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã xóa sách khỏi wishlist thành công")
                .build();
    }

    @DeleteMapping
    public ApiResponse<Void> clearWishlist(Authentication authentication) {
        UUID userId = resolveUserId(authentication);
        wishlistService.clearWishlist(userId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Đã xóa toàn bộ wishlist thành công")
                .build();
    }

    @GetMapping("/check/{bookId}")
    public ApiResponse<BookInWishlistResponse> checkBookInWishlist(@PathVariable UUID bookId,
            Authentication authentication) {
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
