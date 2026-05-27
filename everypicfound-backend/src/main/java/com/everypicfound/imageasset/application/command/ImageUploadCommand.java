package com.everypicfound.imageasset.application.command;

import java.io.InputStream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageUploadCommand {
    
    private InputStream inputStream;

    private String originalFileName;

    private Long fileSize;

    private String mimeType;

    private String fileHash;

    private String fileExt;

    private Integer width;

    private Integer heigth;

    private Long ImageId;
}
