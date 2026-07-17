package com.everypicfound.identity.infrastructure.security.session;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidSessionIdGeneratorTest {

    @Test
    void generatesDifferentUuidSessionIds() {
        UuidSessionIdGenerator generator = new UuidSessionIdGenerator();

        String first = generator.generate();
        String second = generator.generate();

        assertThat(UUID.fromString(first).toString()).isEqualTo(first);
        assertThat(UUID.fromString(second).toString()).isEqualTo(second);
        assertThat(first).isNotEqualTo(second);
    }
}
