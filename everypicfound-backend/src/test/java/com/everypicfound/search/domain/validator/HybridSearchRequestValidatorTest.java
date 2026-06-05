package com.everypicfound.search.domain.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;

class HybridSearchRequestValidatorTest {

    private HybridSearchRequestValidator validator;

    @BeforeEach
    void setUp() {
        SearchProperties searchProperties = new SearchProperties();
        searchProperties.setMaxTopK(50);
        searchProperties.setMaxQueryTextLength(500);
        validator = new HybridSearchRequestValidator(searchProperties);
    }

    @Test
    void validHybridCommandReturnsSuccess() {
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
    void nullQueryImageReturnsSearchImageEmpty() {
        SearchCommand command = validCommand();
        command.setQueryImage(null);

        assertInvalid(command, SearchErrorCode.SEARCH_IMAGE_EMPTY);
    }

    @Test
    void nullQueryImageFileSizeReturnsSearchImageEmpty() {
        SearchCommand command = validCommand();
        command.setQueryImageFileSize(null);

        assertInvalid(command, SearchErrorCode.SEARCH_IMAGE_EMPTY);
    }

    @Test
    void zeroQueryImageFileSizeReturnsSearchImageEmpty() {
        SearchCommand command = validCommand();
        command.setQueryImageFileSize(0L);

        assertInvalid(command, SearchErrorCode.SEARCH_IMAGE_EMPTY);
    }

    @Test
    void nullQueryTextReturnsSearchTextEmpty() {
        SearchCommand command = validCommand();
        command.setQueryText(null);

        assertInvalid(command, SearchErrorCode.SEARCH_TEXT_EMPTY);
    }

    @Test
    void blankQueryTextReturnsSearchTextEmpty() {
        SearchCommand command = validCommand();
        command.setQueryText("   ");

        assertInvalid(command, SearchErrorCode.SEARCH_TEXT_EMPTY);
    }

    @Test
    void queryTextTooLongReturnsSearchTextTooLong() {
        SearchCommand command = validCommand();
        command.setQueryText("a".repeat(501));

        assertInvalid(command, SearchErrorCode.SEARCH_TEXT_TOO_LONG);
    }

    @Test
    void bothImageAndTextMissingReturnsSearchImageEmpty() {
        SearchCommand command = validCommand();
        command.setQueryImage(null);
        command.setQueryImageFileSize(null);
        command.setQueryText(null);

        assertInvalid(command, SearchErrorCode.SEARCH_IMAGE_EMPTY);
    }

    private SearchCommand validCommand() {
        return SearchCommand.builder()
                .searchType(SearchType.HYBRID)
                .topK(10)
                .queryImage(new ByteArrayInputStream(new byte[] {1}))
                .queryImageFileSize(1L)
                .queryText("cat")
                .build();
    }

    private void assertInvalid(SearchCommand command, SearchErrorCode errorCode) {
        SearchValidateResult result = validator.validate(command);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(errorCode);
    }
}
