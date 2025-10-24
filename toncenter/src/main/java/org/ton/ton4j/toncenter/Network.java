package org.ton.ton4j.toncenter;

/** TON Network types supported by TonCenter API */
public enum Network {
  /** TON Mainnet - production network */
  MAINNET("https://toncenter.com/api/v2"),

  /** TON Testnet - test network for development */
  TESTNET("https://testnet.toncenter.com/api/v2"),

  /**
   * <a href="https://github.com/neodix42/mylocalton-docker">MyLocalTon</a> - localhost TON test
   * network for development.
   */
  MY_LOCAL_TON("http://localhost:8082");

  private final String endpoint;

  Network(String endpoint) {
    this.endpoint = endpoint;
  }

  public String getEndpoint() {
    return endpoint;
  }
}
