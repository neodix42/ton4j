package org.ton.ton4j.toncenterv3;

/**
 * Base exception for TonCenter V3 API errors
 */
public class TonCenterException extends RuntimeException {
    
    public TonCenterException(String message) {
        super(message);
    }
    
    public TonCenterException(String message, Throwable cause) {
        super(message, cause);
    }
}
