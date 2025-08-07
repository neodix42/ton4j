package org.ton.ton4j.toncenter;

/**
 * TON Network types supported by TonCenter API
 */
public enum Network {
    /**
     * TON Mainnet - production network
     */
    MAINNET("https://stage.toncenter.com/api/v2"),
    
    /**
     * TON Testnet - test network for development
     */
    TESTNET("https://testnet.toncenter.com/api/v2");
    
    private final String endpoint;
    
    Network(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
}
