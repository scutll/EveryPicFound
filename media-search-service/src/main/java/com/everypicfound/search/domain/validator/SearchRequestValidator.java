package com.everypicfound.search.domain.validator;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.domain.enums.SearchType;

public interface SearchRequestValidator {

    SearchType supportType();

    SearchValidateResult validate(SearchCommand command);
}
