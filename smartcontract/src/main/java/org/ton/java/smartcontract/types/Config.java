package org.ton.java.smartcontract.types;

import java.util.List;

public class Config {
    /**
     * Creation of new locked/restricted packages is only allowed by owner of this (second) public key
     */
    public String configPublicKey;
    /**
     * Whitelist of allowed destinations
     */
    public List<String> allowedDestinations;
}
