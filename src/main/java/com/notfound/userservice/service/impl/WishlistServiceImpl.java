package com.notfound.userservice.service.impl;


import com.notfound.userservice.model.dto.request.AddBookToWishlistRequest;
import com.notfound.userservice.model.dto.response.BookSummaryResponse;
import com.notfound.userservice.model.dto.response.WishlistResponse;
import com.notfound.userservice.model.entity.User;
import com.notfound.userservice.model.entity.Wishlist;
import com.notfound.userservice.repository.UserRepository;
import com.notfound.userservice.repository.WishlistRepository;
import com.notfound.userservice.service.WishlistService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    WishlistRepository wishlistRepository;
    UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public WishlistResponse getMyWishlist(UUID userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        return buildWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public WishlistResponse addBookToWishlist(UUID userId, AddBookToWishlistRequest request) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        if (wishlist.getBookIds() == null) {
            wishlist.setBookIds(new ArrayList<>());
        }

        UUID bookId = request.getBookId();
        if (wishlist.getBookIds().contains(bookId)) {
            // throw new AppException(ErrorCode.BOOK_ALREADY_IN_WISHLIST);
            throw new IllegalArgumentException("Sách đã có trong wishlist");
        }

        wishlist.getBookIds().add(bookId);
        wishlistRepository.save(wishlist);

        log.info("Added book {} to wishlist of user {}", bookId, userId);
        return buildWishlistResponse(wishlist);
    }

    @Override
    @Transactional
    public void removeBookFromWishlist(UUID userId, UUID bookId) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        if (wishlist.getBookIds() == null || wishlist.getBookIds().isEmpty()) {
            // throw new AppException(ErrorCode.WISHLIST_EMPTY);
            throw new IllegalArgumentException("Wishlist đang trống");
        }

        boolean removed = wishlist.getBookIds().remove(bookId);

        if (!removed) {
            // throw new AppException(ErrorCode.BOOK_NOT_IN_WISHLIST);
            throw new IllegalArgumentException("Sách không có trong wishlist");
        }

        wishlistRepository.save(wishlist);
        log.info("Removed book {} from wishlist of user {}", bookId, userId);
    }

    @Override
    @Transactional
    public void clearWishlist(UUID userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);

        if (wishlist.getBookIds() != null) {
            wishlist.getBookIds().clear();
            wishlistRepository.save(wishlist);
            log.info("Cleared wishlist of user {}", userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBookInWishlist(UUID userId, UUID bookId) {
        Optional<Wishlist> wishlistOpt = wishlistRepository.findByUser_Id(userId);

        if (wishlistOpt.isEmpty() || wishlistOpt.get().getBookIds() == null) {
            return false;
        }

        return wishlistOpt.get().getBookIds().contains(bookId);
    }

    private Wishlist getOrCreateWishlist(UUID userId) {
        User user = userRepository.findById(userId)
                // .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        return wishlistRepository.findByUser_Id(userId)
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist(user);
                    return wishlistRepository.save(newWishlist);
                });
    }

    private WishlistResponse buildWishlistResponse(Wishlist wishlist) {
        List<BookSummaryResponse> books = wishlist.getBookIds() != null
                ? wishlist.getBookIds().stream()
                    .map(bookId -> BookSummaryResponse.builder().id(bookId).build())
                    .collect(Collectors.toList())
                : new ArrayList<>();

        return WishlistResponse.builder()
                .wishlistId(wishlist.getWishlistID())
                .userId(wishlist.getUser().getId())
                .createdAt(wishlist.getCreatedAt())
                .books(books)
                .build();
    }
}
