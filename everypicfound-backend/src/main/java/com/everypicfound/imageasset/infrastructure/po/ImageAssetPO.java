package com.everypicfound.imageasset.infrastructure.po;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("image_asset")
public class ImageAssetPO {

    // 主键，图片 ID，同时对应向量库 vector_id。
    @TableId
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

    // 缩略图路径。
    private String thumbnailPath;

    // 图片状态。
    private ImageStatus imageStatus;

    // 向量状态。
    private VectorStatus vectorStatus;

    // 向量完成更新时间。
    private LocalDateTime vectorUpdatedTime;

    // 向量化开始时间。
    private LocalDateTime processingStartedTime;

    // 重试次数。
    private Integer retryCount;

    // 最近失败原因。
    private FailReason failReason;

    // 乐观锁版本号。
    private Integer version;

    // 创建时间。
    private LocalDateTime createdTime;

    // 更新时间。
    private LocalDateTime updatedTime;
}
