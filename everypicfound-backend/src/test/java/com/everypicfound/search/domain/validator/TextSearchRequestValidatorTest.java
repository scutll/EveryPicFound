package com.everypicfound.search.domain.validator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;

class TextSearchRequestValidatorTest {

    private TextSearchRequestValidator validator;

    @BeforeEach
    void setUp() {
        SearchProperties searchProperties = new SearchProperties();
        searchProperties.setMaxTopK(50);
        searchProperties.setMaxQueryTextLength(500);
        validator = new TextSearchRequestValidator(searchProperties);
    }

    @Test
    void validTextCommandReturnsSuccess() {
        SearchValidateResult result = validator.validate(validCommand());

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void nullCommandReturnsSearchTypeInvalid() {
        assertInvalid(null, SearchErrorCode.SEARCH_TYPE_INVALID);
    }

    @Test
    void wrongSearchTypeReturnsSearchTypeInvalid() {
        SearchCommand command = validCommand();
        command.setSearchType(SearchType.IMAGE);

        assertInvalid(command, SearchErrorCode.SEARCH_TYPE_INVALID);
    }

    @Test
    void nullTopKReturnsSearchTopKInvalid() {
        SearchCommand command = validCommand();
        command.setTopK(null);

        assertInvalid(command, SearchErrorCode.SEARCH_TOPK_INVALID);
    }

    @Test
    void nullTopKDoesNotFillDefaultTopK() {
        SearchCommand command = validCommand();
        command.setTopK(null);

        SearchValidateResult result = validator.validate(command);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(SearchErrorCode.SEARCH_TOPK_INVALID);
        assertThat(command.getTopK()).isNull();
    }

    @Test
    void zeroTopKReturnsSearchTopKInvalid() {
        SearchCommand command = validCommand();
        command.setTopK(0);

        assertInvalid(command, SearchErrorCode.SEARCH_TOPK_INVALID);
    }

    @Test
    void negativeTopKReturnsSearchTopKInvalid() {
        SearchCommand command = validCommand();
        command.setTopK(-1);

        assertInvalid(command, SearchErrorCode.SEARCH_TOPK_INVALID);
    }

    @Test
    void topKGreaterThanMaxTopKReturnsSearchTopKInvalid() {
        SearchCommand command = validCommand();
        command.setTopK(51);

        assertInvalid(command, SearchErrorCode.SEARCH_TOPK_INVALID);
    }

    @Test
    void nullQueryTextReturnsSearchTextEmpty() {
        SearchCommand command = validCommand();
        command.setQueryText(null);

        assertInvalid(command, SearchErrorCode.SEARCH_TEXT_EMPTY);
    }

    @Test
    void emptyQueryTextReturnsSearchTextEmpty() {
        SearchCommand command = validCommand();
        command.setQueryText("");

        assertInvalid(command, SearchErrorCode.SEARCH_TEXT_EMPTY);
    }

    @Test
    void blankQueryTextReturnsSearchTextEmpty() {
        SearchCommand command = validCommand();
        command.setQueryText("   ");

        assertInvalid(command, SearchErrorCode.SEARCH_TEXT_EMPTY);
    }

    @Test
    void queryTextWithSurroundingWhitespaceIsNotTrimmedBackToCommand() {
        SearchCommand command = validCommand();
        command.setQueryText("  cat  ");

        SearchValidateResult result = validator.validate(command);

        assertThat(result.isValid()).isTrue();
        assertThat(command.getQueryText()).isEqualTo("  cat  ");
    }

    @Test
    void queryTextLengthEqualMaxQueryTextLengthReturnsSuccess() {
        SearchCommand command = validCommand();
        command.setQueryText("a".repeat(500));

        SearchValidateResult result = validator.validate(command);

        assertThat(result.isValid()).isTrue();
        assertThat(result.getErrorCode()).isNull();
    }

    @Test
    void queryTextLengthGreaterThanMaxQueryTextLengthReturnsSearchTextTooLong() {
        SearchCommand command = validCommand();
        command.setQueryText("a".repeat(501));

        assertInvalid(command, SearchErrorCode.SEARCH_TEXT_TOO_LONG);
    }

    private SearchCommand validCommand() {
        return SearchCommand.builder()
                .searchType(SearchType.TEXT)
                .topK(10)
                .queryText("cat")
                .build();
    }

    private void assertInvalid(SearchCommand command, SearchErrorCode errorCode) {
        SearchValidateResult result = validator.validate(command);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(errorCode);
    }
}
