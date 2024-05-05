package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;
import java.util.List;

@Builder
@Getter
@Setter
@ToString
public class HighloadConfig implements WalletConfig {
    public BigInteger queryId;
    public List<Destination> destinations;
}
