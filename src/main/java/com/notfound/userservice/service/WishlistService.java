package com.notfound.userservice.service;

import com.notfound.userservice.model.dto.request.AddBookToWishlistRequest;
import com.notfound.userservice.model.dto.response.WishlistResponse;

import java.util.UUID;

public interface WishlistService {
    WishlistResponse getMyWishlist(UUID userId);
    WishlistResponse addBookToWishlist(UUID userId, AddBookToWishlistRequest request);
    void removeBookFromWishlist(UUID userId, UUID bookId);
    void clearWishlist(UUID userId);
    boolean isBookInWishlist(UUID userId, UUID bookId);
}
