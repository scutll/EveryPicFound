package com.everypicfound.search.domain.validator;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageSearchRequestValidator implements SearchRequestValidator {

    private final SearchProperties searchProperties;

    @Override
    public SearchType supportType() {
        return SearchType.IMAGE;
    }

    @Override
    public SearchValidateResult validate(SearchCommand command) {
        if (command == null) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_PARAM_INVALID);
        }

        if (command.getSearchType() != SearchType.IMAGE) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TYPE_INVALID);
        }

        if (command.getTopK() == null || command.getTopK() <= 0) {
            command.setTopK(searchProperties.getDefaultTopK());
        }

        if (isInvalidTopK(command.getTopK())) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TOPK_INVALID);
        }

        if (command.getQueryImage() == null
                || command.getQueryImageFileSize() == null
                || command.getQueryImageFileSize() <= 0) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_IMAGE_EMPTY);
        }

        if (command.getQueryImageMimeType() == null) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_PARAM_INVALID, "Image Mime Type null");
        }

        return SearchValidateResult.success();
    }

    private boolean isInvalidTopK(Integer topK) {
        return topK == null || topK <= 0 || topK > searchProperties.getMaxTopK();
    }
}
