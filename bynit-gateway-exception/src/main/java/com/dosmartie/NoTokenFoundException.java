package com.dosmartie;


import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class NoTokenFoundException extends ResponseStatusException {
    public NoTokenFoundException() {
        super(HttpStatus.UNAUTHORIZED, "Token not found in headers");
    }
}
