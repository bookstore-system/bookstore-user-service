package com.notfound.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service sử dụng thuật toán RS256 (RSA + SHA-256).
 *
 * - User Service: dùng PRIVATE KEY để KÝ (sign) token.
 * - API Gateway: dùng PUBLIC KEY để XÁC THỰC (verify) token.
 */
@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${APP_JWT_PRIVATE_KEY}") String privateKeyStr,
            @Value("${APP_JWT_PUBLIC_KEY}") String publicKeyStr,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");

            // Private key dùng để ký token (PKCS#8)
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyStr);
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            this.privateKey = keyFactory.generatePrivate(privateKeySpec);

            // Public key dùng để verify token (X.509)
            byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyStr);
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            this.publicKey = keyFactory.generatePublic(publicKeySpec);

            this.expirationMs = expirationMs;
            this.refreshExpirationMs = refreshExpirationMs;
        } catch (Exception e) {
            throw new IllegalStateException("Không thể load RSA keypair từ environment variables", e);
        }
    }

    /**
     * Tạo JWT token được ký bằng RS256 (private key).
     * Token chứa subject (username), role, và thời gian hết hạn.
     */
    public String generateToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Tạo Refresh Token (thời gian sống dài hơn, không chứa extra claims).
     */
    public String generateRefreshToken(String subject) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshExpirationMs);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    /**
     * Tạo JWT token với custom claims (ví dụ: role, userId).
     */
    public String generateToken(String subject, Map<String, Object> extraClaims) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(now)
                .expiration(exp)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    /**
     * Trích xuất subject (username) từ token.
     * Sử dụng public key để verify signature.
     */
    public String extractSubject(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String subject = extractSubject(token);
        return subject != null
                && subject.equalsIgnoreCase(userDetails.getUsername())
                && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Parse và verify JWT bằng PUBLIC KEY (RS256).
     * Nếu signature không hợp lệ hoặc token hết hạn → throw Exception.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    /**
     * Lấy public key (dùng để share cho API Gateway nếu cần).
     */
    public PublicKey getPublicKey() {
        return publicKey;
    }
}
