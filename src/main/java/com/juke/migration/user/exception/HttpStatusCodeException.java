package com.juke.migration.user.exception;

/**
 * Represents an HTTP error with a certain status code.
 *
 * @author Philipp Kumar
 */
public class HttpStatusCodeException extends Exception {

    private final int statusCode;

    public HttpStatusCodeException(int statusCode) {
        this.statusCode = statusCode;
    }

    public HttpStatusCodeException(int statusCode, Exception cause) {
        super(cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

}
