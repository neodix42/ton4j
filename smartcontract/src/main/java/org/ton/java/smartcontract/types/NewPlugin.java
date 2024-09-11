package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Data
public class NewPlugin {
    public byte[] secretKey;
    public long seqno;
    public long pluginWc;
    public BigInteger amount;
    public Cell stateInit;
    public Cell body;
}
