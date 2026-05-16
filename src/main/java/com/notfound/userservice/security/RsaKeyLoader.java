package com.notfound.userservice.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Đọc RSA key từ thư mục PEM (mặc định {@code /key}).
 * Hỗ trợ: PRIVATE KEY (PKCS#8), PUBLIC KEY (SPKI), CERTIFICATE (X.509).
 */
public final class RsaKeyLoader {

    private RsaKeyLoader() {
    }

    public static PrivateKey loadPrivateKey(Path keysDir, String fileName) {
        try {
            byte[] der = readKeyBytes(keysDir.resolve(fileName));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Không thể load private key từ " + keysDir.resolve(fileName), e);
        }
    }

    public static PublicKey loadPublicKey(Path keysDir, String fileName) {
        Path file = keysDir.resolve(fileName);
        try {
            String pem = Files.readString(file);
            if (pem.contains("BEGIN CERTIFICATE")) {
                return loadPublicKeyFromCertificate(pem);
            }
            byte[] der = decodePem(pem);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Không thể load public key từ " + file, e);
        }
    }

    public static Path resolveKeysDir(String keysDir) {
        Path path = Path.of(keysDir);
        if (!path.isAbsolute()) {
            path = Path.of(System.getProperty("user.dir")).resolve(path).normalize();
        }
        if (!Files.isDirectory(path)) {
            throw new IllegalStateException("Thư mục JWT keys không tồn tại: " + path);
        }
        return path;
    }

    private static PublicKey loadPublicKeyFromCertificate(String pem) throws Exception {
        byte[] der = decodePem(pem);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (var is = new ByteArrayInputStream(der)) {
            X509Certificate cert = (X509Certificate) cf.generateCertificate(is);
            return cert.getPublicKey();
        }
    }

    private static byte[] readKeyBytes(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            throw new IOException("File không tồn tại: " + file);
        }
        String pem = Files.readString(file);
        if (pem.contains("BEGIN CERTIFICATE")) {
            throw new IOException("Certificate không dùng làm private key: " + file);
        }
        return decodePem(pem);
    }

    private static byte[] decodePem(String pem) {
        String normalized = pem.trim();
        for (String label : new String[]{"PRIVATE KEY", "PUBLIC KEY", "CERTIFICATE"}) {
            if (normalized.contains("BEGIN " + label)) {
                String base64 = normalized
                        .replace("-----BEGIN " + label + "-----", "")
                        .replace("-----END " + label + "-----", "")
                        .replaceAll("\\s+", "");
                return Base64.getDecoder().decode(base64);
            }
        }
        return Base64.getDecoder().decode(normalized.replaceAll("\\s+", ""));
    }
}
