package org.ton.ton4j.toncenterv3;

import lombok.Getter;

/**
 * Exception for API-level errors returned by TonCenter V3
 */
@Getter
public class TonCenterApiException extends TonCenterException {
    
    private final Integer errorCode;
    
    public TonCenterApiException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
