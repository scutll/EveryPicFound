package com.everypicfound.search.domain.validator;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.config.SearchProperties;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.error.SearchErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TextSearchRequestValidator implements SearchRequestValidator {

    private final SearchProperties searchProperties;

    @Override
    public SearchType supportType() {
        return SearchType.TEXT;
    }

    @Override
    public SearchValidateResult validate(SearchCommand command) {
        if (command == null) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_PARAM_INVALID);
        }

        if (command.getSearchType() != SearchType.TEXT) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TYPE_INVALID);
        }

        if (command.getTopK() == null || command.getTopK() <= 0) {
            command.setTopK(searchProperties.getDefaultTopK());
        }

        if (isInvalidTopK(command.getTopK())) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TOPK_INVALID);
        }

        if (command.getQueryText() == null || command.getQueryText().trim().isEmpty()) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TEXT_EMPTY);
        }

        if (command.getQueryText().trim().length() > searchProperties.getMaxQueryTextLength()) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TEXT_TOO_LONG);
        }

        return SearchValidateResult.success();
    }

    private boolean isInvalidTopK(Integer topK) {
        return topK == null || topK <= 0 || topK > searchProperties.getMaxTopK();
    }
}
