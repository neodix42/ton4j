package org.ton.ton4j.toncenter;

/**
 * Exception thrown when the TonCenter API returns an error response (ok=false)
 */
public class TonCenterApiException extends TonCenterException {
    
    private final Integer errorCode;
    
    public TonCenterApiException(String message, Integer errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public Integer getErrorCode() {
        return errorCode;
    }
    
    @Override
    public String getMessage() {
        if (errorCode != null) {
            return String.format("API Error [%d]: %s", errorCode, super.getMessage());
        }
        return super.getMessage();
    }
}
