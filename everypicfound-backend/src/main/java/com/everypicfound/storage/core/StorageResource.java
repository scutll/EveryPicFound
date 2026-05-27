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
public class StorageResource {

    // 文件输入流。
    private InputStream inputStream;

    // 文件大小。
    private Long fileSize;

    // 文件扩展名。
    private String fileExt;

    // MIME 类型。
    private String mimeType;

    // 前端访问图片的地址。
    private String accessUrl;
}
