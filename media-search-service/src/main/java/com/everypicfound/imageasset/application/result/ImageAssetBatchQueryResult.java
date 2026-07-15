package com.everypicfound.imageasset.application.result;

import com.everypicfound.imageasset.application.dto.ImageAssetDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageAssetBatchQueryResult {

    // 批量回表得到的图片元数据列表。
    private List<ImageAssetDTO> items;
}
