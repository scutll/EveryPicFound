package com.everypicfound.storage.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.InputStream;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class StorageSaveRequest {
    
    private InputStream inputStream;

    private String originalFileName;

    private String fileExt;

    private Long fileSize;

    private Long imageId;

    private String mimeType;

}
