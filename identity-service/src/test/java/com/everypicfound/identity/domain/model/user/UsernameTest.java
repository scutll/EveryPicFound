package com.everypicfound.identity.domain.model.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UsernameTest {

    @Test
    void shouldStripLeadingAndTrailingWhitespace() {
        Username username = Username.of("\u2003User_01\u2003");

        assertThat(username.value()).isEqualTo("User_01");
    }

    @Test
    void shouldPreserveCaseAndTreatDifferentCaseAsDifferentValues() {
        Username upperCaseUsername = Username.of("User01");
        Username lowerCaseUsername = Username.of("user01");

        assertThat(upperCaseUsername.value()).isEqualTo("User01");
        assertThat(lowerCaseUsername.value()).isEqualTo("user01");
        assertThat(upperCaseUsername).isNotEqualTo(lowerCaseUsername);
    }


    @ParameterizedTest
    @ValueSource(strings={
        "abc",
        "ABC",
        "a1b",
        "a_b",
        "User_01",
        "a1_b2_C3"
        })

    void shouldAcceptValidUsername(String rawUsername) {
        assertThatCode(() -> Username.of(rawUsername)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings=
        {"_abc",
            "abc_",
            "a__b",
            "ab cd",
            "用户123",
            "abc-123",
            "abc.def",
            "abc\nxyz",
            "abc😀"}
    )
    void shouldRejectInvalidUsernameFormats(String rawUsername) {
        assertThatThrownBy(() -> Username.of(rawUsername))
                .isInstanceOfSatisfying(
                        InvalidUsernameException.class,
                        exception -> assertThat(exception.violation())
                                .isEqualTo(UsernameViolation.FORMAT));
    }


    @ParameterizedTest
    @ValueSource(strings = { "", " " })
    void shouldRejectMissingUsernames(String rawUsername) {
        assertThatThrownBy(() -> Username.of(rawUsername))
                .isInstanceOfSatisfying(
                        InvalidUsernameException.class,
                        exception -> assertThat(exception.violation())
                                .isEqualTo(UsernameViolation.REQUIRED));
    }

    @Test
    void shouldRejectUsernamesShorterThanThreeCharacters(
            ) {
        assertThatThrownBy(() -> Username.of("ab"))
                .isInstanceOfSatisfying(
                        InvalidUsernameException.class,
                        exception -> assertThat(exception.violation())
                                .isEqualTo(UsernameViolation.LENGTH));
    }

    @Test
    void shouldAcceptThirtyTwoCharacterUsername() {
        assertThatCode(() -> Username.of("a".repeat(32)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUsernameLongerThanThirtyTwoCharacters() {
        assertThatThrownBy(() -> Username.of("a".repeat(33)))
                .isInstanceOfSatisfying(
                        InvalidUsernameException.class,
                        exception -> assertThat(exception.violation())
                                .isEqualTo(UsernameViolation.LENGTH));
    }

    @Test
    void shouldRejectNullUsername() {
        assertThatThrownBy(() -> Username.of(null))
                .isInstanceOfSatisfying(
                        InvalidUsernameException.class,
                        exception -> assertThat(exception.violation())
                                .isEqualTo(UsernameViolation.REQUIRED));
    }

    @Test
    void shouldBeEqualWhenNormalizedValuesAreEqual() {
        Username first = Username.of("  User_01  ");
        Username second = Username.of("User_01");

        assertThat(first).isEqualTo(second);
    }
}
