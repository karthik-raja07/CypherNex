package com.cyphernex.forge;

public class GroqApiException extends RuntimeException {

    private final int statusCode;

    public GroqApiException(String message) {
        super(message);
        this.statusCode = 0;
    }

    public GroqApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GroqApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
