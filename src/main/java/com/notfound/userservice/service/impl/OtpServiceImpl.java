package com.notfound.userservice.service.impl;

import com.notfound.userservice.service.OtpService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class OtpServiceImpl implements OtpService {

    StringRedisTemplate redisTemplate;
    static String OTP_PREFIX = "otp:";
    static long OTP_EXPIRY_MINUTES = 5;

    @Override
    public String generateOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        String key = OTP_PREFIX + email;

        log.info("Generating OTP for email: {}. OTP: {}", email, otp);
        
        redisTemplate.opsForValue().set(key, otp, OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);
        
        return otp;
    }

    @Override
    public boolean verifyOtp(String email, String otp) {
        String key = OTP_PREFIX + email;
        String storedOtp = redisTemplate.opsForValue().get(key);
        
        log.info("Verifying OTP for email: {}. Expected: {}, Provided: {}", email, storedOtp, otp);
        
        return otp != null && otp.equals(storedOtp);
    }

    @Override
    public void deleteOtp(String email) {
        String key = OTP_PREFIX + email;
        redisTemplate.delete(key);
        log.info("Deleted OTP for email: {}", email);
    }
}
