package com.everypicfound.imageasset.domain.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import com.everypicfound.common.log.LogService;

@Service
@RequiredArgsConstructor
public class LogOrphanFileLogService implements OrphanFileLogService {
    
    private final LogService LogService;

    @Override
    public void recordOrphanFile(OrphanFileRecord record) {
        throw new UnsupportedOperationException("TODO");
    }
}
