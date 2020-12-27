package com.tlvlp.iot.server.mcu;

public class UnitException extends RuntimeException {
    public UnitException(String message) {
        super(message);
    }

    public UnitException(String message, Throwable cause) {
        super(message, cause);
    }
}
