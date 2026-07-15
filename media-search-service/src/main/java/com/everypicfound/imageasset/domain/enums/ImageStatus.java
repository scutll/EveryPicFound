package com.everypicfound.imageasset.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ImageStatus {
    NORMAL(1, "normal picture asset"),
                    
    DELETED(2, "picture deleted"),
                    
    INVALID(3, "error picture asset");

    private final Integer code;

    private final String description;
}
