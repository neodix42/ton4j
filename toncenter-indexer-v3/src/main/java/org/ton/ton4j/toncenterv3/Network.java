package org.ton.ton4j.toncenterv3;

import lombok.Getter;

/** Network enum for TonCenter V3 API endpoints */
@Getter
public enum Network {
  MAINNET("https://toncenter.com/api/v3"),
  TESTNET("https://testnet.toncenter.com/api/v3"),
  /**
   * <a href="https://github.com/ton-blockchain/mylocalton-docker">MyLocalTon</a> - localhost TON test
   * network for development.
   */
  MY_LOCAL_TON("http://localhost:8081/api/v3");

  private final String endpoint;

  Network(String endpoint) {
    this.endpoint = endpoint;
  }
}
