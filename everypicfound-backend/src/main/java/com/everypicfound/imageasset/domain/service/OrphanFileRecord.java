package com.everypicfound.imageasset.domain.service;

import com.everypicfound.imageasset.domain.enums.CleanStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 孤儿文件记录。
 *
 * <p>当前仅用于记录日志，不负责落库、删除、重试或定时清理。</p>
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrphanFileRecord {

    /**
     * 图片 ID。
     */
    private Long imageId;

    /**
     * 文件逻辑存储路径。
     */
    private String storagePath;

    /**
     * 文件访问地址。
     */
    private String accessUrl;

    /**
     * 存储后的文件名。
     */
    private String fileName;

    /**
     * 原始文件名。
     */
    private String originalFileName;

    /**
     * 文件 hash。
     */
    private String fileHash;

    /**
     * 失败原因。
     */
    private String failReason;

    /**
     * 清理重试次数。
     */
    private Integer retryCount;

    /**
     * 清理状态。
     */
    private CleanStatus cleanStatus;
}
