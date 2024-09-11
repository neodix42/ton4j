package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;

import java.math.BigInteger;

@Builder
@Data
public class DeployedPlugin {
    public byte[] secretKey;
    public long seqno;
    public Address pluginAddress;
    public BigInteger amount;
    public int queryId;

}
