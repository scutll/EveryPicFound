package com.everypicfound.imageasset.application.command;

import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadCommand {

    // 图片文件输入流。
    private InputStream inputStream;

    // 用户原始文件名。
    private String originalFileName;

    // 文件大小。
    private Long fileSize;

    // MIME 类型。
    private String mimeType;

    // 文件 hash。
    private String fileHash;

    // 文件扩展名。
    private String fileExt;

    // 图片宽度。
    private Integer width;

    // 图片高度。
    private Integer height;

    // 图片 ID。
    private Long imageId;
}
