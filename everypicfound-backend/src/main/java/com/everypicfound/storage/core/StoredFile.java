package com.everypicfound.storage.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    // 图片存储路径。
    private String storagePath;

    // 文件扩展名。
    private String fileExt;

    // 文件大小。
    private Long fileSize;

    // 前端访问图片的地址。
    private String accessUrl;

    // MIME 类型。
    private String mimeType;
}
