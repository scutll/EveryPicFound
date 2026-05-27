package com.everypicfound.imageasset.domain.generator;

import com.everypicfound.imageasset.application.command.ImageUploadCommand;

public interface FileHashCalculator {

    // 计算文件内容 hash。
    String calculateHash(ImageUploadCommand command);
}
