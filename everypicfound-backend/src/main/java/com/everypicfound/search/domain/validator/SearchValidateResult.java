package com.everypicfound.search.domain.validator;

import com.everypicfound.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchValidateResult {

    private final boolean valid;

    private final ErrorCode errorCode;

    private final String message;

    public static SearchValidateResult success() {
        return new SearchValidateResult(true, null, null);
    }

    public static SearchValidateResult fail(ErrorCode errorCode) {
        return new SearchValidateResult(false, errorCode, null);
    }

    public static SearchValidateResult fail(ErrorCode errorCode, String message) {
        return new SearchValidateResult(false, errorCode, message);
    }
}
