package com.everypicfound.imageasset.application.dto;

import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAssetDTO {

    // 图片 ID。
    private Long id;

    // 系统生成文件名。
    private String fileName;

    // 用户原始文件名。
    private String originalFileName;

    // 文件 hash。
    private String fileHash;

    // 文件大小。
    private Long fileSize;

    // MIME 类型。
    private String mimeType;

    // 文件扩展名。
    private String fileExt;

    // 图片宽度。
    private Integer width;

    // 图片高度。
    private Integer height;

    // 图片存储路径。
    private String storagePath;

    // 图片访问地址, 数据库中并没有url字段，弃用
    // private String imageUrl;

    // 缩略图路径。
    private String thumbnailPath;

    // 图片状态。
    private ImageStatus imageStatus;

    // 向量状态。
    private VectorStatus vectorStatus;

    // 创建时间。
    private LocalDateTime createdTime;

    // 更新时间。
    private LocalDateTime updatedTime;

    private LocalDateTime vectorUpdatedTime;

    private LocalDateTime processingStartedTime;

    private Integer retryCount;

    private FailReason failReason;

    private Integer version;

}
