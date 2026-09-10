package com.resumeoptimizer.resume_optimizer.exception;

/** Thrown when a refresh token is missing, unknown, expired, or revoked. */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
