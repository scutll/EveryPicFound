package com.everypicfound.imageasset.domain.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.everypicfound.imageasset.domain.enums.CleanStatus;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrphanFileRecord {
    
    private String storagePath;

    private String fileHash;

    private String failReason;

    private Integer retryCount;

    private CleanStatus CleanStatus;
}
