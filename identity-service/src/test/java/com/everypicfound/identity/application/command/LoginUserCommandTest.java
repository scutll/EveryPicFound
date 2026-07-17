package com.everypicfound.identity.application.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginUserCommandTest {

    @Test
    void protectsRawPasswordWhenRenderedAsText() {
        LoginUserCommand command = new LoginUserCommand(
                "User_01",
                "secret123");

        assertThat(command.toString())
                .contains("username=User_01")
                .contains("rawPassword=PROTECTED")
                .doesNotContain("secret123");
    }
}
