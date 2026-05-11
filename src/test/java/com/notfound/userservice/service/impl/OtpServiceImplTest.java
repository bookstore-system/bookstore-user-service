package com.notfound.userservice.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OtpServiceImplTest {

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OtpServiceImpl otpService;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        //noinspection unchecked
        valueOperations = (ValueOperations<String, String>) mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        otpService = new OtpServiceImpl(redisTemplate);
    }

    @Test
    void generateOtp_setsRedisWithExpiryAndReturns6Digits() {
        String email = "test@example.com";

        String otp = otpService.generateOtp(email);

        assertNotNull(otp);
        assertTrue(otp.matches("\\d{6}"), "OTP must be 6 digits");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> ttlCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<TimeUnit> unitCaptor = ArgumentCaptor.forClass(TimeUnit.class);

        verify(valueOperations, times(1))
                .set(keyCaptor.capture(), otpCaptor.capture(), ttlCaptor.capture(), unitCaptor.capture());

        assertEquals("otp:" + email, keyCaptor.getValue());
        assertEquals(otp, otpCaptor.getValue());
        assertEquals(5L, ttlCaptor.getValue());
        assertEquals(TimeUnit.MINUTES, unitCaptor.getValue());
    }

    @Test
    void verifyOtp_returnsTrueWhenMatches() {
        String email = "test@example.com";
        when(valueOperations.get("otp:" + email)).thenReturn("123456");

        assertTrue(otpService.verifyOtp(email, "123456"));
    }

    @Test
    void verifyOtp_returnsFalseWhenNullOrMismatch() {
        String email = "test@example.com";
        when(valueOperations.get("otp:" + email)).thenReturn("123456");

        assertFalse(otpService.verifyOtp(email, null));
        assertFalse(otpService.verifyOtp(email, "000000"));
    }

    @Test
    void deleteOtp_deletesRedisKey() {
        String email = "test@example.com";

        otpService.deleteOtp(email);

        verify(redisTemplate).delete("otp:" + email);
    }
}

