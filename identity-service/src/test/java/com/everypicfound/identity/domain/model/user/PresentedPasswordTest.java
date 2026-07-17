package com.everypicfound.identity.domain.model.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PresentedPasswordTest {

    @Test
    void treatsStoredCredentialInputAsOpaqueSensitiveText() {
        PresentedPassword password = PresentedPassword.of("short");

        assertThat(password.value()).isEqualTo("short");
        assertThat(password.toString())
                .isEqualTo("PresentedPassword[PROTECTED]")
                .doesNotContain("short");
    }

    @Test
    void rejectsMissingCredential() {
        assertThatThrownBy(() -> PresentedPassword.of(null))
                .isInstanceOf(InvalidPasswordException.class)
                .extracting(exception ->
                        ((InvalidPasswordException) exception).violation())
                .isEqualTo(PasswordViolation.REQUIRED);
        assertThatThrownBy(() -> PresentedPassword.of(""))
                .isInstanceOf(InvalidPasswordException.class);
    }
}
