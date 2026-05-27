package com.everypicfound.imageasset.domain.service;

public interface OrphanFileLogService {

    // 记录文件已保存但 MySQL 入库失败的情况。
    void recordOrphanFile(OrphanFileRecord record);
}
