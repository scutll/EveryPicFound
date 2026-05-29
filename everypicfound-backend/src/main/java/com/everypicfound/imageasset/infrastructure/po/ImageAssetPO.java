package com.everypicfound.imageasset.infrastructure.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("image_asset")
public class ImageAssetPO {

    @TableId(value = "id", type = IdType.INPUT)
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