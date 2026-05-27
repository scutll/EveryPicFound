package com.everypicfound.imageasset.infrastructure.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAssetPO {

    private Long id;

    private String fileName;

    private String originalFileName;

    private String fileHash;

    private Long fileSize;

    private String mimeType;

    private String fileExt;

    private Integer width;

    private Integer height;

    private String storagePath;

    private String thumbnailPath;

    private Integer imageStatus;

    private Integer vectorStatus;

    private LocalDateTime vectorUpdatedTime;

    private LocalDateTime processingStartedTime;

    private Integer retryCount;

    private String failReason;

    private Integer version;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}