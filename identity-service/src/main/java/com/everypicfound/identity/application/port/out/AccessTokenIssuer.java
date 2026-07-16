package com.everypicfound.identity.application.port.out;

import com.everypicfound.identity.application.command.AccessTokenIssueRequest;
import com.everypicfound.identity.application.result.IssuedAccessToken;

/**
 * Access Token 签发出站端口。
 */
public interface AccessTokenIssuer {

    IssuedAccessToken issue(AccessTokenIssueRequest request);
}
