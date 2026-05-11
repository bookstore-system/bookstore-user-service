package com.notfound.userservice.exception;

/**
 * Thrown when a client requests image upload but Cloudinary (or configured storage) is not available.
 */
public class ImageStorageUnavailableException extends RuntimeException {

    public ImageStorageUnavailableException(String message) {
        super(message);
    }
}
