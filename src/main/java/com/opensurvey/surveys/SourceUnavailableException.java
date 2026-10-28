package com.opensurvey.surveys;

public class SourceUnavailableException extends RuntimeException {
    private final String source;

    public SourceUnavailableException(String source) {
        super("Source " + source + " is unavailable.");
        this.source = source;
    }

    public String source() {
        return source;
    }
}
