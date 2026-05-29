package com.everypicfound.imageasset.domain.duplicate;

import com.everypicfound.common.exception.BizException;
import com.everypicfound.common.exception.CommonErrorCode;
import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import com.everypicfound.imageasset.error.ImageAssetErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultImageDuplicateChecker implements ImageDuplicateChecker {

    private final ImageAssetRepository imageAssetRepository;

    @Override
    public void checkDuplicate(String fileHash) {
        if (fileHash == null || fileHash.isBlank()) {
            throw new BizException(CommonErrorCode.PARAM_ERROR);
        }

        boolean exists = imageAssetRepository.existsByFileHash(fileHash);
        if (exists) {
            throw new BizException(ImageAssetErrorCode.DUPLICATE_IMAGE);
        }

    }

    @Override
    public boolean existsByFileHash(String fileHash) {
        if (fileHash == null || fileHash.isBlank()) {
            return false;
        }

        return imageAssetRepository.existsByFileHash(fileHash);
    }
}