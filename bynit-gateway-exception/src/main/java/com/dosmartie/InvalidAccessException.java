package com.dosmartie;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.server.ResponseStatusException;

public class InvalidAccessException extends ResponseStatusException {
    public InvalidAccessException() {
        super(HttpStatus.FORBIDDEN, "Invalid Access");
    }
}
