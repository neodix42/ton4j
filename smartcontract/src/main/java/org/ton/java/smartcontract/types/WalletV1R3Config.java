package org.ton.java.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;
import org.ton.java.tonlib.types.ExtraCurrency;

@Builder
@Data
public class WalletV1R3Config implements WalletConfig {
    Boolean bounce;
    long seqno;
    int mode;
    Address destination;
    BigInteger amount;
    List<ExtraCurrency> extraCurrencies;
    Cell body;
    StateInit stateInit;
    String comment;
}
