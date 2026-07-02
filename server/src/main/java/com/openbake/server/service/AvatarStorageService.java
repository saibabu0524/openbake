package com.openbake.server.service;

import com.openbake.server.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/** Local-disk avatar storage, mirroring backend/app/routers/profile.py's upload_avatar (no Cloudinary in use today). */
@Service
public class AvatarStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    private final Path mediaDir;

    public AvatarStorageService(@Value("${app.media-dir:./media}") String mediaDirProperty) {
        this.mediaDir = Path.of(mediaDirProperty, "avatars");
    }

    public String store(String userId, MultipartFile file, String existingUrl) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, and WebP images are allowed");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Image must be smaller than 5MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = (originalName != null && originalName.contains("."))
                ? originalName.substring(originalName.lastIndexOf('.') + 1)
                : "jpg";
        String filename = userId + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        try {
            Files.createDirectories(mediaDir);
            if (existingUrl != null && !existingUrl.isBlank()) {
                String oldFilename = existingUrl.substring(existingUrl.lastIndexOf('/') + 1);
                Files.deleteIfExists(mediaDir.resolve(oldFilename));
            }
            Files.copy(file.getInputStream(), mediaDir.resolve(filename));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save avatar image");
        }

        return "/media/avatars/" + filename;
    }
}
