package com.notfound.userservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.notfound.userservice.exception.ImageStorageUnavailableException;
import com.notfound.userservice.service.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageServiceImpl implements ImageService {

    private final ObjectProvider<Cloudinary> cloudinaryProvider;

    @Override
    public Map<String, Object> uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        Cloudinary cloudinary = cloudinaryProvider.getIfAvailable();
        if (cloudinary == null) {
            throw new ImageStorageUnavailableException(
                    "Upload ảnh cần bật Cloudinary: đặt cloudinary.enabled=true và cloudinary.cloud-name, api-key, api-secret (hoặc biến môi trường tương ứng).");
        }

        if (folder == null || folder.isBlank()) {
            folder = "bookstore/avatars";
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = (Map<String, Object>) cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "image",
                            "overwrite", true,
                            "invalidate", true
                    )
            );

            log.info("Image uploaded successfully to {}: {}", folder, uploadResult.get("url"));
            return uploadResult;
        } catch (IOException e) {
            log.error("Error uploading image to {}: {}", folder, e.getMessage());
            throw new RuntimeException("Failed to upload image: " + e.getMessage(), e);
        }
    }
}
