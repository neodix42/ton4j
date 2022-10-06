package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class DeployedPlugin {
    public byte[] secretKey;
    public long seqno;
    public Address pluginAddress;
    public BigInteger amount;
    public int queryId;

}
