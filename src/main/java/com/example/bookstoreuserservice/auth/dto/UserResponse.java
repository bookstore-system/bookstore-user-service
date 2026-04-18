package com.example.bookstoreuserservice.auth.dto;

import java.time.Instant;

public record UserResponse(Long id, String email, String fullName, Instant createdAt) {}
