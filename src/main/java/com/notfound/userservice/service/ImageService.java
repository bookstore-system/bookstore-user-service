package com.notfound.userservice.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ImageService {

    /**
     * Upload một ảnh lên Cloudinary.
     *
     * @param folder ví dụ {@code bookstore/avatars}
     */
    Map<String, Object> uploadImage(MultipartFile file, String folder);
}
