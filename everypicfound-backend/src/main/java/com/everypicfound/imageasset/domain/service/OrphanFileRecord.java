package com.everypicfound.imageasset.domain.service;

import com.everypicfound.imageasset.domain.enums.CleanStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrphanFileRecord {

    // 图片存储路径。
    private String storagePath;

    // 文件 hash。
    private String fileHash;

    // 失败原因。
    private String failReason;

    // 重试次数。
    private Integer retryCount;

    // 孤儿文件清理状态预留。
    private CleanStatus cleanStatus;
}
