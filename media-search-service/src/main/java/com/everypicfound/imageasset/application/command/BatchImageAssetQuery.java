package com.everypicfound.imageasset.application.command;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImageAssetQuery {

    // 图片 ID 列表。
    private List<Long> imageIds;
}
