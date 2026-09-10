package com.ai_resume.dashboard_service.exception;

/** Raised when a dependency the dashboard genuinely cannot do without is down. */
public class UpstreamUnavailableException extends RuntimeException {

    private final String source;

    public UpstreamUnavailableException(String source, Throwable cause) {
        super(source + " is unavailable", cause);
        this.source = source;
    }

    public String getSource() {
        return source;
    }
}

