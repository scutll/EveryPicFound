package com.everypicfound.imageasset.domain.generator;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public class Sha256FileHashCalculator implements FileHashCalculator {

    // 计算文件 SHA-256 hash。
    @Override
    public String calculateHash(ImageUploadCommand command) {
        throw new UnsupportedOperationException("TODO");
    }
}
