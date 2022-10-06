package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class NewPlugin {
    public byte[] secretKey;
    public long seqno;
    public long pluginWc;
    public BigInteger amount;
    public Cell stateInit;
    public Cell body;
}
