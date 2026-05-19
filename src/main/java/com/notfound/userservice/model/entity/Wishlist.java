package com.notfound.userservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wishlists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"user", "bookIds"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Wishlist {

    @Id
    @UuidGenerator
    UUID wishlistID;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    /**
     * Trong kiến trúc microservice, Wishlist chỉ lưu bookId (UUID).
     * Book detail sẽ được lấy qua OpenFeign từ Book Service.
     */
    @ElementCollection
    @CollectionTable(name = "wishlist_books", joinColumns = @JoinColumn(name = "wishlist_id"))
    @Column(name = "book_id")
    List<UUID> bookIds;

    public Wishlist(User user) {
        this.user = user;
    }
}
