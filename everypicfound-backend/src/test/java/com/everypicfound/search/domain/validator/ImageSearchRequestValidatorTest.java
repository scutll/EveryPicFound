package com.everypicfound.search.domain.validator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;

class ImageSearchRequestValidatorTest {

    private ImageSearchRequestValidator validator;

    @BeforeEach
    void setUp() {
        SearchProperties searchProperties = new SearchProperties();
        searchProperties.setMaxTopK(50);
        validator = new ImageSearchRequestValidator(searchProperties);
    }

    @Test
    void validImageCommandReturnsSuccess() {
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
        command.setSearchType(SearchType.TEXT);

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
    void validateDoesNotReadQueryImageInputStream() {
        CountingInputStream inputStream = new CountingInputStream();
        SearchCommand command = validCommand();
        command.setQueryImage(inputStream);

        SearchValidateResult result = validator.validate(command);

        assertThat(result.isValid()).isTrue();
        assertThat(inputStream.getReadCount()).isZero();
    }

    private SearchCommand validCommand() {
        return SearchCommand.builder()
                .searchType(SearchType.IMAGE)
                .topK(10)
                .queryImage(new ByteArrayInputStream(new byte[] {1}))
                .queryImageFileSize(1L)
                .build();
    }

    private void assertInvalid(SearchCommand command, SearchErrorCode errorCode) {
        SearchValidateResult result = validator.validate(command);

        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorCode()).isEqualTo(errorCode);
    }

    private static class CountingInputStream extends InputStream {

        private int readCount;

        @Override
        public int read() throws IOException {
            readCount++;
            return -1;
        }

        private int getReadCount() {
            return readCount;
        }
    }
}
