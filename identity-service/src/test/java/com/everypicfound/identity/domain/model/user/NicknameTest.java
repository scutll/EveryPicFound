package com.everypicfound.identity.domain.model.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NicknameTest {

    @Test
    void shouldTreatNullAsNoNickname() {
        assertThat(Nickname.optionalOf(null)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "　"})
    void shouldTreatBlankValueAsNoNickname(String value) {
        assertThat(Nickname.optionalOf(value)).isEmpty();
    }

    @Test
    void shouldStripLeadingAndTrailingWhitespace() {
        Optional<Nickname> nickname =
                Nickname.optionalOf("  图片收藏家  ");

        assertThat(nickname)
                .hasValueSatisfying(value ->
                        assertThat(value.value())
                                .isEqualTo("图片收藏家"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "A",
            "图片 收藏家",
            "摄影师📷",
            "家庭相册👨‍👩‍👧‍👦"
    })
    void shouldAcceptPrintableUnicode(String value) {
        assertThatCode(() -> Nickname.optionalOf(value))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptThirtyTwoCodePointNickname() {
        assertThatCode(() -> Nickname.optionalOf("😀".repeat(32)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNicknameLongerThanThirtyTwoCodePoints() {
        assertViolation(
                "😀".repeat(33),
                NicknameViolation.LENGTH);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "名称\n换行",
            "名称\t制表",
            "名称\u2028换行"
    })
    void shouldRejectControlAndLineBreakCharacters(String value) {
        assertViolation(
                value,
                NicknameViolation.INVALID_CHARACTER);
    }

    @Test
    void shouldCompareUsingNormalizedValue() {
        Nickname first = Nickname.optionalOf("  图友  ").orElseThrow();
        Nickname second = Nickname.optionalOf("图友").orElseThrow();

        assertThat(first).isEqualTo(second);
    }

    private void assertViolation(
            String value,
            NicknameViolation expectedViolation) {
        assertThatThrownBy(() -> Nickname.optionalOf(value))
                .isInstanceOfSatisfying(
                        InvalidNicknameException.class,
                        exception -> assertThat(exception.violation())
                                .isEqualTo(expectedViolation));
    }
}
