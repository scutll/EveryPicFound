package com.everypicfound.storage.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredFile {

    private String storagePath;

    private String accessUrl;

    private String fileName;

    private String fileExt;

    private String mimeType;

    private Long fileSize;
}
