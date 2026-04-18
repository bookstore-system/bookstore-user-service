package com.example.bookstoreuserservice.auth.dto;

public record LoginResponse(
        String token, String type, long expiresIn, UserResponse user) {}
