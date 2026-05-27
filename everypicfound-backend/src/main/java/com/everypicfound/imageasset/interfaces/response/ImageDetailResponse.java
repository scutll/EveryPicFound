package com.everypicfound.imageasset.interfaces.response;

import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDetailResponse {

    // 图片 ID。
    private Long id;

    // 系统生成文件名。
    private String fileName;

    // 用户原始文件名。
    private String originalFileName;

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

    // 图片状态。
    private ImageStatus imageStatus;

    // 向量状态。
    private VectorStatus vectorStatus;

    // 图片访问地址。
    private String imageUrl;
}
