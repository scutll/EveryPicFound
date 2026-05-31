package com.everypicfound.vectorindex.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VectorDeleteRequest {

    private String collectionName;

    private Long vectorId;
}