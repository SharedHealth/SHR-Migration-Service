package org.sharedhealth.migrationservice.exception;

import java.io.IOException;

public class ConnectionException extends IOException {

    private final int errorCode;

    public ConnectionException(String message, int errorCode, Throwable e) {
        super(message, e);
        this.errorCode = errorCode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
