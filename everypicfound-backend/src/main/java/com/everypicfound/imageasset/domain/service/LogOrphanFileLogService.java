package com.everypicfound.imageasset.domain.service;

import com.everypicfound.common.log.LogService;

public class LogOrphanFileLogService implements OrphanFileLogService {

    // 孤儿文件记录对象。
    private OrphanFileRecord orphanFileRecord;

    // 记录孤儿文件日志。
    private LogService logService;

    // MVP 阶段记录孤儿文件日志。
    @Override
    public void recordOrphanFile(OrphanFileRecord record) {
        throw new UnsupportedOperationException("TODO");
    }
}
