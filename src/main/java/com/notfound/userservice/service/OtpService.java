package com.notfound.userservice.service;

public interface OtpService {
    /**
     * Sinh mã OTP ngẫu nhiên 6 chữ số
     */
    String generateOtp(String email);

    /**
     * Xác thực mã OTP
     */
    boolean verifyOtp(String email, String otp);

    /**
     * Xóa mã OTP sau khi đã sử dụng
     */
    void deleteOtp(String email);
}
