package com.everypicfound.imageasset.interfaces.controller;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.springframework.web.bind.annotation.RestController;

import com.everypicfound.common.exception.SystemException;
import com.everypicfound.storage.api.FileStorageService;
import com.everypicfound.storage.core.StorageResource;
import com.everypicfound.storage.error.StorageErrorCode;
import com.everypicfound.storage.infrastructure.config.StorageProperties;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.http.ResponseEntity;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;


@RestController
@RequiredArgsConstructor
public class ImageFileController {
    
    private final FileStorageService fileStorageService;

    private final StorageProperties storageProperties;

    @GetMapping("/images/**")
    public ResponseEntity<StreamingResponseBody> getImage(HttpServletRequest request) {
        String storagePath = resolveStoragePath(request);
        StorageResource resource = fileStorageService.read(storagePath);

        StreamingResponseBody body = outputStream -> {
            try(InputStream inputStream = resource.getInputStream()){
                inputStream.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(resolveMediaType(resource.getMimeType()))
                .contentLength(resource.getFileSize() == null ? -1 : resource.getFileSize())
                .cacheControl(CacheControl.noCache())
                .body(body);
    }

    private String resolveStoragePath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();

        if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }

        String accessUrlPrefix = normalizeAccessUrlPrefix(storageProperties.getAccessUrlPrefix());

        if (!requestUri.startsWith(accessUrlPrefix + "/")) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }

        String encodedStoragePath = requestUri.substring((accessUrlPrefix + "/").length());
        String storagePath = URLDecoder.decode(encodedStoragePath, StandardCharsets.UTF_8);

        if (storagePath.isBlank() || storagePath.contains("..")) {
            throw new SystemException(StorageErrorCode.STORAGE_PATH_INVALID);
        }

        return storagePath;
    }

    private String normalizeAccessUrlPrefix(String accessUrlPrefix) {
        if (accessUrlPrefix == null || accessUrlPrefix.isBlank()) {
            return "/image";
        }

        String result = accessUrlPrefix.trim();

        if (!result.startsWith("/")) {
            result = "/" + result;
        }

        while (result.endsWith("/") && result.length() > 1) {
            result = result.substring(0, result.length() - 1);
        }

        return result;
    }

    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        return MediaType.parseMediaType(mimeType);
    }
    
}
