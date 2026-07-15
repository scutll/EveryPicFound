package com.everypicfound.search.domain.validator;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.error.SearchErrorCode;


@Component
public class SearchValidatorManager {

    private final Map<SearchType, SearchRequestValidator> validatorMap;

    public SearchValidatorManager(List<SearchRequestValidator> validators) {
        this.validatorMap = validators.stream()
                .collect(Collectors.toMap(SearchRequestValidator::supportType, Function.identity()));
    }

    public SearchValidateResult validate(SearchCommand command) {

        if (command == null) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_PARAM_INVALID);
        }

        if (command.getSearchType() == null) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TYPE_INVALID);
        }

        SearchRequestValidator validator = validatorMap.get(command.getSearchType());

        if (validator == null) {
            return SearchValidateResult.fail(SearchErrorCode.SEARCH_TYPE_INVALID);
        }

        return validator.validate(command);
    }
}
