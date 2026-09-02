package com.hrms.common.exception;

import org.springframework.http.HttpStatus;

public class TokenRefreshException extends ApiException {

    public TokenRefreshException(String token, String message) {
        super(String.format("Failed for token [%s]: %s", token, message), HttpStatus.FORBIDDEN);
    }

    public TokenRefreshException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
