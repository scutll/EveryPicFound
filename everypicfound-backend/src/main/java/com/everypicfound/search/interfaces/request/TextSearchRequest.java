package com.everypicfound.search.interfaces.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TextSearchRequest {

    private String queryText;

    private Integer topK;
}
