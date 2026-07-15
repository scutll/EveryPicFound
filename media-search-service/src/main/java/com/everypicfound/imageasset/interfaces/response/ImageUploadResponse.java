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
public class ImageUploadResponse {

    // 图片 ID。
    private Long imageId;

    // 用户原始文件名。
    private String originalFileName;

    // 图片访问地址。
    private String imageUrl;

    // 图片状态。
    private ImageStatus imageStatus;

    // 向量状态。
    private VectorStatus vectorStatus;
}
