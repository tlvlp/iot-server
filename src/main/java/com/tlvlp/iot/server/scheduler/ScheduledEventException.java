package com.tlvlp.iot.server.scheduler;

public class ScheduledEventException extends RuntimeException {
    public ScheduledEventException(String message) {
        super(message);
    }

    public ScheduledEventException(String message, Throwable cause) {
        super(message, cause);
    }
}
