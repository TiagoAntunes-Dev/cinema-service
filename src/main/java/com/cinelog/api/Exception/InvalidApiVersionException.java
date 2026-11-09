package com.cinelog.api.Exception;

public class InvalidApiVersionException extends RuntimeException {
    public InvalidApiVersionException(String message) {
        super(message);
    }
}
