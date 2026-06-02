package com.everypicfound.system.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ComponentHealthResult {
    
    private String component;

    private Boolean healthy;

    private Long costMs;

    private String message;
}
