package com.everypicfound.imageasset.application.command;

import com.everypicfound.imageasset.domain.enums.ImageStatus;
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
public class ImageAssetQueryCriteria {

    // 当前页码。
    private Integer pageNo;

    // 每页大小。
    private Integer pageSize;

    // 图片状态。
    private ImageStatus imageStatus;

    // 向量状态。
    private VectorStatus vectorStatus;

    // 创建开始时间。
    private LocalDateTime createdStartTime;

    // 创建结束时间。
    private LocalDateTime createdEndTime;
}
