package com.everypicfound.identity.application.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterUserCommandTest {

    @Test
    void doesNotExposeRawPasswordFromToString() {
        RegisterUserCommand command = new RegisterUserCommand(
                "User01",
                "secret123",
                "探索者");

        assertThat(command.toString())
                .doesNotContain("secret123")
                .contains("PROTECTED");
    }
}
