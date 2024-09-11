package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class LockupConfig {
    /**
     * Creation of new locked/restricted packages is only allowed by owner of this (second) public key
     */
    public String configPublicKey;
    /**
     * Whitelist of allowed destinations
     */
    public List<String> allowedDestinations;

    public BigInteger totalRestrictedValue;
    public BigInteger totalLockedValue;
}
