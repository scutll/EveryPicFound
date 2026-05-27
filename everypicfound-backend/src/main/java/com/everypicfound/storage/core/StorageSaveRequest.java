package com.everypicfound.storage.core;

import java.io.InputStream;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StorageSaveRequest {

    // 文件输入流。
    private InputStream inputStream;

    // 用户原始文件名，只用于日志记录，不参与文件逻辑路径生成。
    private String originalFileName;

    // 文件扩展名。
    private String fileExt;

    // 文件大小。
    private Long fileSize;

    // 图片 ID，由 image-asset 模块在保存文件前提前生成。
    private Long imageId;

    // MIME 类型。
    private String mimeType;
}
