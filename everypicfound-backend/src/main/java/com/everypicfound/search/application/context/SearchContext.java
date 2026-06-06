package com.everypicfound.search.application.context;

import com.everypicfound.search.application.command.SearchCommand;
import com.everypicfound.search.domain.enums.SearchType;
import com.everypicfound.search.domain.validator.SearchValidateResult;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SearchContext {

    private SearchCommand command;

    private SearchType searchType;

    private Integer topK;

    private Long startTime;

    private Long costMs;

    private SearchValidateResult validateResult;
}
