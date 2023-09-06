package com.dosmartie;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class SessionTimeOutException extends ResponseStatusException {
    public SessionTimeOutException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
