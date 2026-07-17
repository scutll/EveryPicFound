package com.everypicfound.identity.infrastructure.security.token;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Sha256RefreshTokenHasherTest {

    @Test
    void hashesRefreshTokenWithSha256Hex() {
        Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

        String hash = hasher.hash("refresh-token-raw");

        assertThat(hash).isEqualTo(
                "5e4b06c757a1d6a5a10b96b0a1c1af9f9ef85e82c64967bfd724133436f58d16");
    }

    @Test
    void rejectsBlankTokenValue() {
        Sha256RefreshTokenHasher hasher = new Sha256RefreshTokenHasher();

        assertThatThrownBy(() -> hasher.hash(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenValue");
    }
}
