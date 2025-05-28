package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;

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
