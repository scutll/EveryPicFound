package com.everypicfound.imageasset.domain.validator;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Component;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.imageasset.application.command.ImageUploadCommand;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;
import com.everypicfound.storage.infrastructure.config.StorageProperties;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DefaultImageUploadValidator implements ImageUploadValidator {

    private static final Set<String> DEFAULT_ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> DEFAULT_ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_FILE_EXT_LENGTH = 20;

    private final StorageProperties storageProperties;

    @Override
    public void validate(ImageUploadCommand command) {
        validateFileNotEmpty(command);
        validateFileSize(command);
        validateFileExt(command);
        validateMimeType(command);
        validateImageReadable(command);
    }

    public void validateFileNotEmpty(ImageUploadCommand command) {
        if (command == null || command.getInputStream() == null
                || command.getFileSize() == null || command.getFileSize() <= 0) {
            throw new BizException(ImageAssetErrorCode.IMAGE_EMPTY);
        }
    }

    public void validateFileSize(ImageUploadCommand command) {
        Long maxFileSize = storageProperties.getMaxFileSize();
        if (maxFileSize != null && command.getFileSize() > maxFileSize) {
            throw new BizException(ImageAssetErrorCode.IMAGE_SIZE_EXCEEDED);
        }
    }

    public void validateFileExt(ImageUploadCommand command) {
        String fileExt = resolveFileExt(command);
        if (fileExt.isBlank()
                || fileExt.length() > MAX_FILE_EXT_LENGTH
                || !isAlphaNumeric(fileExt)
                || !allowedExtensions().contains(fileExt)) {
            throw new BizException(ImageAssetErrorCode.IMAGE_FORMAT_UNSUPPORTED);
        }
    }

    public void validateMimeType(ImageUploadCommand command) {
        String mimeType = normalizeMimeType(command.getMimeType());
        if (mimeType.isBlank() || !DEFAULT_ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new BizException(ImageAssetErrorCode.IMAGE_MIME_INVALID);
        }
    }

    public void validateImageReadable(ImageUploadCommand command) {
        BufferedInputStream bufferedInputStream = toBufferedInputStream(command.getInputStream());
        command.setInputStream(bufferedInputStream);
        bufferedInputStream.mark(readLimit(command));

        try {
            BufferedImage image = ImageIO.read(bufferedInputStream);
            if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                throw new BizException(ImageAssetErrorCode.IMAGE_DECODE_FAILED);
            }
        } catch (IOException e) {
            throw new BizException(ImageAssetErrorCode.IMAGE_DECODE_FAILED, e);
        } finally {
            resetInputStream(bufferedInputStream);
        }
    }

    private BufferedInputStream toBufferedInputStream(InputStream inputStream) {
        if (inputStream instanceof BufferedInputStream bufferedInputStream) {
            return bufferedInputStream;
        }
        return new BufferedInputStream(inputStream);
    }

    private void resetInputStream(BufferedInputStream bufferedInputStream) {
        try {
            bufferedInputStream.reset();
        } catch (IOException e) {
            throw new BizException(ImageAssetErrorCode.IMAGE_DECODE_FAILED, e);
        }
    }

    private String resolveFileExt(ImageUploadCommand command) {
        if (command.getFileExt() != null && !command.getFileExt().isBlank()) {
            return normalizeFileExt(command.getFileExt());
        }

        String originalFileName = command.getOriginalFileName();
        if (originalFileName == null || originalFileName.isBlank()) {
            return "";
        }

        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == originalFileName.length() - 1) {
            return "";
        }
        return normalizeFileExt(originalFileName.substring(lastDotIndex + 1));
    }

    private String normalizeFileExt(String fileExt) {
        if (fileExt == null) {
            return "";
        }
        String normalized = fileExt.trim().toLowerCase(Locale.ROOT);
        return normalized.startsWith(".") ? normalized.substring(1) : normalized;
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private Set<String> allowedExtensions() {
        List<String> configuredExtensions = storageProperties.getAllowedExtensions();
        if (configuredExtensions == null || configuredExtensions.isEmpty()) {
            return DEFAULT_ALLOWED_EXTENSIONS;
        }

        return configuredExtensions.stream()
                .map(this::normalizeFileExt)
                .filter(fileExt -> !fileExt.isBlank())
                .filter(fileExt -> fileExt.length() <= MAX_FILE_EXT_LENGTH)
                .filter(this::isAlphaNumeric)
                .collect(Collectors.toSet());
    }

    private boolean isAlphaNumeric(String fileExt) {
        for (int i = 0; i < fileExt.length(); i++) {
            if (!isAsciiLetterOrDigit(fileExt.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isAsciiLetterOrDigit(char ch) {
        return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9');
    }

    private int readLimit(ImageUploadCommand command) {
        Long fileSize = command.getFileSize();
        if (fileSize == null || fileSize >= Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return Math.toIntExact(fileSize + 1);
    }
}
