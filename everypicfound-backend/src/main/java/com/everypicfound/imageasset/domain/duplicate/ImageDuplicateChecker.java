package com.everypicfound.imageasset.domain.duplicate;

public interface ImageDuplicateChecker {
    
    void checkDuplicate(String fileHash);

    boolean existsByFileHash(String fileHash);
}
