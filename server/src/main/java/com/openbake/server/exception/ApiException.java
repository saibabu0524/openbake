package com.openbake.server.exception;

import org.springframework.http.HttpStatus;

/** Thrown by services/controllers to produce a FastAPI-style {"detail": "..."} error response. */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String detail) {
        super(detail);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
