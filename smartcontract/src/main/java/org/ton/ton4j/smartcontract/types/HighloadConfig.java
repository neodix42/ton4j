package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Builder
@Data
public class HighloadConfig implements WalletConfig {
    public long walletId;
    public BigInteger queryId;
    public List<Destination> destinations;
}
