package org.dao.exceptions;

public class dDBWriteFailedException extends RuntimeException {
    
    public dDBWriteFailedException(String message) {
        super(message);
    }

    public dDBWriteFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
