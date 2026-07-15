package com.everypicfound.imageasset.domain.service;

/**
 * 孤儿文件日志记录服务。
 *
 * <p>用于记录图片上传链路中文件已经保存成功、元数据保存失败，
 * 且补偿删除文件也失败后产生的孤儿文件信息。</p>
 *
 * <p>当前接口只定义记录能力，不负责删除文件、重试清理、落库或发送 MQ。</p>
 */
public interface OrphanFileLogService {

    /**
     * 记录孤儿文件信息。
     *
     * @param record 孤儿文件记录
     */
    void recordOrphanFile(OrphanFileRecord record);
}
