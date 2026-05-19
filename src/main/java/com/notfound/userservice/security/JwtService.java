package com.notfound.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

/**
 * JWT Service sử dụng thuật toán RS256 (RSA + SHA-256).
 *
 * - User Service: dùng PRIVATE KEY trong {@code /key/private.pem} để ký token.
 * - API Gateway: dùng PUBLIC KEY trong {@code /key/public.pem} để verify.
 */
@Service
public class JwtService {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${app.jwt.keys-dir:/key}") String keysDir,
            @Value("${app.jwt.private-key-file:private.pem}") String privateKeyFile,
            @Value("${app.jwt.public-key-file:public.pem}") String publicKeyFile,
            @Value("${app.jwt.expiration-ms:86400000}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMs) {
        Path dir = RsaKeyLoader.resolveKeysDir(keysDir);
        this.privateKey = RsaKeyLoader.loadPrivateKey(dir, privateKeyFile);
        this.publicKey = RsaKeyLoader.loadPublicKey(dir, publicKeyFile);
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

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

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
