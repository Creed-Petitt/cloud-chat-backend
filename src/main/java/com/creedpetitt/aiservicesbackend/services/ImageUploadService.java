package com.creedpetitt.aiservicesbackend.services;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class ImageUploadService {

    private final Storage storage;

    @Value("${spring.cloud.gcp.storage.bucket}")
    private String bucketName;

    public ImageUploadService(Storage storage) {
        this.storage = storage;
    }

    public String uploadUserImage(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("File type cannot be determined");
        }

        // Support images and PDFs
        boolean isImage = contentType.startsWith("image/");
        boolean isPdf = contentType.equals("application/pdf");
        
        if (!isImage && !isPdf) {
            throw new IllegalArgumentException("File must be an image (JPEG, PNG, WebP, GIF) or PDF");
        }

        // Validate file size (max 10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "uploads/" + UUID.randomUUID() + extension;

        // Upload to GCS
        BlobId blobId = BlobId.of(bucketName, filename);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.create(blobInfo, file.getBytes());

        // Return public URL
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, filename);
    }
}
