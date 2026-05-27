package com.everypicfound.imageasset.application.command;

import com.everypicfound.imageasset.domain.enums.ImageStatus;
import com.everypicfound.imageasset.domain.enums.VectorStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImageAssetSaveCommand {

    private Long imageId;

    private String fileName;

    private String originalFileName;

    private String fileHash;

    private Long fileSize;

    private String mimeType;

    private String fileExt;

    private Integer width;

    private Integer height;

    private String storagePath;

    private String thumbnailPath;

    private ImageStatus imageStatus;

    private VectorStatus vectorStatus;

}
