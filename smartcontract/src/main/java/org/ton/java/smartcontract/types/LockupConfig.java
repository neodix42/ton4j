package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class LockupConfig {
    /**
     * Creation of new locked/restricted packages is only allowed by owner of this (second) public key
     */
    public String configPublicKey;
    /**
     * Whitelist of allowed destinations
     */
    public List<String> allowedDestinations = new ArrayList<>();
}
