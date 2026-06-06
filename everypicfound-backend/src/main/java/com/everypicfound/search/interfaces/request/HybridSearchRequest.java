package com.everypicfound.search.interfaces.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HybridSearchRequest {

    private MultipartFile queryImage;

    private String queryText;

    private Integer topK;
}
