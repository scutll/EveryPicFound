package com.everypicfound.imageasset.application.command;

import com.everypicfound.imageasset.domain.enums.FailReason;
import com.everypicfound.imageasset.domain.enums.ImageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageStatusUpdateCommand {

    // 图片 ID。
    private Long imageId;

    // 目标图片状态。
    private ImageStatus targetStatus;

    // 失败原因。
    private FailReason failReason;

    // 乐观锁版本号。
    private Integer version;
}
