package com.ai_resume.notification_service.exception;

/** Thrown when an email fails to send. Always caught internally - never allowed
 * to propagate as a 5xx that would suggest the caller's business operation failed. */
public class EmailDeliveryException extends RuntimeException {

    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}

