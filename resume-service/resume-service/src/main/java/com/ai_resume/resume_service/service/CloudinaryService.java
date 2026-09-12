package com.ai_resume.resume_service.service;

import com.ai_resume.resume_service.dto.CloudinaryUploadResponse;
import com.ai_resume.resume_service.exception.FileProcessingException;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Uploads already-read file bytes.
     *
     * <p>Taking {@code byte[]} instead of {@code MultipartFile} is deliberate: the
     * caller reads the upload exactly once and shares those bytes with the text
     * extractor. The previous version called {@code file.getBytes()} a second time
     * here, which re-buffered (or, for streamed multipart resolvers, failed on) an
     * already-consumed request part.
     */
    public CloudinaryUploadResponse upload(byte[] content, String fileName, String folder) {
        if (content == null || content.length == 0) {
            throw new FileProcessingException("Cannot upload an empty file: " + fileName, null);
        }
        try {
            Map<?, ?> response = cloudinary.uploader().upload(
                    content,
                    ObjectUtils.asMap(
                            "folder", folder,
                            "resource_type", "raw",
                            // A random suffix guarantees uniqueness. Previously two users
                            // uploading "resume.pdf" produced the same public_id and the
                            // second upload silently overwrote the first one's file.
                            "public_id", uniquePublicId(fileName),
                            "overwrite", false));
            log.debug("Uploaded '{}' to Cloudinary folder '{}'", fileName, folder);
            return toUploadResponse(response);
        } catch (Exception ex) {
            log.error("Cloudinary upload failed for '{}' in folder '{}'", fileName, folder, ex);
            throw new FileProcessingException("Failed to upload file to Cloudinary: " + fileName, ex);
        }
    }

    /** Uploads plain text (used when a job description is pasted rather than uploaded). */
    public CloudinaryUploadResponse uploadText(String text, String fileName, String folder) {
        return upload(text.getBytes(StandardCharsets.UTF_8), fileName, folder);
    }

    /**
     * Best-effort delete. Failures are logged rather than thrown so a Cloudinary
     * outage cannot block the user's database delete, but they ARE surfaced in the
     * logs because every failure here leaks storage (and money).
     */
    public void deleteByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            log.debug("Deleted Cloudinary asset {}", publicId);
        } catch (Exception ex) {
            log.warn("ORPHANED ASSET: failed to delete Cloudinary resource '{}' — "
                    + "it must be cleaned up manually", publicId, ex);
        }
    }

    /**
     * Downloads a previously uploaded asset through the backend so the browser
     * receives the correct content type and the user's authorization is checked
     * by the API before the asset is returned.
     */
    public byte[] download(String secureUrl) {
        if (secureUrl == null || secureUrl.isBlank()) {
            throw new FileProcessingException("The original file is not available", null);
        }

        URI uri;
        try {
            uri = URI.create(secureUrl);
        } catch (IllegalArgumentException ex) {
            throw new FileProcessingException("The stored original file URL is invalid", ex);
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"res.cloudinary.com".equalsIgnoreCase(uri.getHost())) {
            throw new FileProcessingException("The stored original file URL is not trusted", null);
        }

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    HttpRequest.newBuilder(uri)
                            .timeout(Duration.ofSeconds(30))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FileProcessingException(
                        "Cloudinary returned HTTP " + response.statusCode()
                                + " while loading the original file",
                        null);
            }
            return response.body();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new FileProcessingException("Loading the original file was interrupted", ex);
        } catch (java.io.IOException ex) {
            throw new FileProcessingException("Could not load the original file", ex);
        }
    }

    private CloudinaryUploadResponse toUploadResponse(Map<?, ?> response) {
        Object secureUrl = response.get("secure_url");
        Object publicId = response.get("public_id");
        if (secureUrl == null || publicId == null) {
            throw new FileProcessingException(
                    "Cloudinary response did not contain secure_url/public_id", null);
        }
        return CloudinaryUploadResponse.builder()
                .url(secureUrl.toString())
                .publicId(publicId.toString())
                .build();
    }

//    private String uniquePublicId(String name) {
//        String base = (name == null || name.isBlank()) ? "document" : name;
//        String sanitized = base
//                .replaceAll("\\.[^.]+$", "")
//                .replaceAll("[^a-zA-Z0-9_-]", "-");
//        if (sanitized.isBlank()) {
//            sanitized = "document";
//        }
//        if (sanitized.length() > 80) {
//            sanitized = sanitized.substring(0, 80);
//        }
//        return sanitized + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
//    }
//}
private String uniquePublicId(String name) {

    String base = (name == null || name.isBlank()) ? "document" : name;

    String extension = "";

    int dot = base.lastIndexOf('.');

    if (dot != -1) {
        extension = base.substring(dot);
        base = base.substring(0, dot);
    }

    String sanitized = base
            .replaceAll("\\.[^.]+$", "")
            .replaceAll("[^a-zA-Z0-9_-]", "-");

    if (sanitized.isBlank()) {
        sanitized = "document";
    }

    if (sanitized.length() > 80) {
        sanitized = sanitized.substring(0, 80);
    }
//    return sanitized + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);

    return sanitized + "-"
            + UUID.randomUUID().toString().replace("-", "").substring(0, 12)
            + extension;
}
}
