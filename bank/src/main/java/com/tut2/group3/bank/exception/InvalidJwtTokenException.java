package com.tut2.group3.bank.exception;

/**
 * Signals that a JWT token failed validation or could not be parsed.
 */
public class InvalidJwtTokenException extends Exception {

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidJwtTokenException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidJwtTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
