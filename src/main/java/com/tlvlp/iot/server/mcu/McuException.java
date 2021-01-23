package com.tlvlp.iot.server.mcu;

public class McuException extends RuntimeException {
    public McuException(String message) {
        super(message);
    }

    public McuException(String message, Throwable cause) {
        super(message, cause);
    }
}
