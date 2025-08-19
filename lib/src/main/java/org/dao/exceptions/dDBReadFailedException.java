package org.dao.exceptions;

public class dDBReadFailedException extends RuntimeException {
    
    public dDBReadFailedException(String message) {
        super(message);
    }

    public dDBReadFailedException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
