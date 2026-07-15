package com.everypicfound.system.health;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthSnapshot {
    
    private Boolean healthy;

    private LocalDateTime checkTime;

    private List<ComponentHealthResult> components;
}
