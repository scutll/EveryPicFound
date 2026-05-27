package com.everypicfound.imageasset.application.command;

import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.VectorStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorStatusUpdateCommand {

    // 图片 ID。
    private Long imageId;

    // 目标向量状态。
    private VectorStatus targetStatus;

    // 向量化开始时间。
    private LocalDateTime processingStartedTime;

    // 向量完成更新时间。
    private LocalDateTime vectorUpdatedTime;

    // 重试次数。
    private Integer retryCount;

    // 失败原因。
    private FailReason failReason;

    // 乐观锁版本号。
    private Integer version;
}
