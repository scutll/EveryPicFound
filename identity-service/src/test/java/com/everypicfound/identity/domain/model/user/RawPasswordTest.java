package com.everypicfound.identity.domain.model.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "123456",
            "aaaaaa",
            "中文密码测试",
            "😀😀😀😀😀😀"
    })
    void shouldAcceptPasswordsWithoutCombinationRequirements(String value) {
        assertThatCode(() -> RawPassword.of(value))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldPreserveOriginalUnicodeSequence() {
        String value = "éabcde";

        RawPassword password = RawPassword.of(value);

        assertThat(password.value()).isEqualTo(value);
    }

    @Test
    void shouldRejectNullPasswordAsRequired() {
        assertViolation(null, PasswordViolation.REQUIRED);
    }

    @Test
    void shouldRejectEmptyPasswordAsRequired() {
        assertViolation("", PasswordViolation.REQUIRED);
    }

    @Test
    void shouldRejectPasswordShorterThanSixCodePoints() {
        assertViolation("abcde", PasswordViolation.LENGTH);
    }

    @Test
    void shouldRejectPasswordLongerThanTwentyFiveCodePoints() {
        assertViolation("a".repeat(26), PasswordViolation.LENGTH);
    }

    @Test
    void shouldAcceptPasswordAtTwentyFiveCodePointBoundary() {
        assertThatCode(() -> RawPassword.of("a".repeat(25)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptPasswordAtSeventyTwoUtf8ByteBoundary() {
        assertThatCode(() -> RawPassword.of("😀".repeat(18)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectPasswordLongerThanSeventyTwoUtf8Bytes() {
        assertViolation(
                "😀".repeat(19),
                PasswordViolation.UTF8_TOO_LONG);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc def",
            "abcde　f"
    })
    void shouldRejectWhitespace(String value) {
        assertViolation(value, PasswordViolation.WHITESPACE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "abc\tdef",
            "abc\ndef"
    })
    void shouldRejectControlCharacters(String value) {
        assertViolation(value, PasswordViolation.CONTROL_CHARACTER);
    }

    @Test
    void shouldNotExposePasswordFromToString() {
        RawPassword password = RawPassword.of("secret123");

        assertThat(password.toString())
                .doesNotContain("secret123");
    }

    private void assertViolation(
            String value,
            PasswordViolation expectedViolation) {
        assertThatThrownBy(() -> RawPassword.of(value))
                .isInstanceOfSatisfying(
                        InvalidPasswordException.class,
                        exception -> assertThat(exception.violation())
                                .isEqualTo(expectedViolation));
    }
}
