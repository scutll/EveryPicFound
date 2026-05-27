package com.everypicfound.imageasset.domain.generator;

import org.springframework.stereotype.Component;

@Component
public class SnowflakeImageGenerator implements ImageIdGenerator {

    @Override
    public Long nextId() {
        throw new UnsupportedOperationException("TODO");
    }
}
