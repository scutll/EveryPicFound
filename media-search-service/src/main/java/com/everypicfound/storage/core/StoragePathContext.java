package com.everypicfound.storage.core;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoragePathContext {

    // 图片 ID。
    private Long imageId;

    // 文件扩展名。
    private String fileExt;

    // 上传时间。
    private LocalDateTime uploadTime;
}
