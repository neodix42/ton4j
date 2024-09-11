package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;

@Builder
@Data
public class HighloadV3Config implements WalletConfig {
    long walletId;
    int mode;
    int queryId;
    long createdAt;
    StateInit stateInit;
    Cell body;
    long timeOut;
}
