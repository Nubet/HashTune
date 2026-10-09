package com.norbertfila.hashtune.exceptions.security;

import com.norbertfila.hashtune.exceptions.ErrorCode;
import com.norbertfila.hashtune.exceptions.application.ApplicationException;

public class AuthenticationRequiredException extends ApplicationException {
    public AuthenticationRequiredException() {
        super(ErrorCode.AUTHENTICATION_REQUIRED, "Authentication is required to access this resource.");
    }
}
