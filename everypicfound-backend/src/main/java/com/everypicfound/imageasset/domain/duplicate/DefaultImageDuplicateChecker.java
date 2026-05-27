package com.everypicfound.imageasset.domain.duplicate;

import com.everypicfound.imageasset.domain.repository.ImageAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DefaultImageDuplicateChecker implements ImageDuplicateChecker {

    private final ImageAssetRepository imageAssetRepository;

    @Override
    public void checkDuplicate(String fileHash) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public boolean existsByFileHash(String fileHash) {
        throw new UnsupportedOperationException("TODO");
    }
}