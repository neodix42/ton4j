package org.ton.ton4j.toncenter;

/** Base exception for all TonCenter API related errors */
public class TonCenterException extends RuntimeException {

  public TonCenterException(String message) {
    super(message);
  }

  public TonCenterException(String message, Throwable cause) {
    super(message, cause);
  }
}
