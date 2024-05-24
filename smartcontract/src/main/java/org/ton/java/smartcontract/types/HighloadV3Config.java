package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.tlb.types.StateInit;

@Builder
@Getter
@Setter
@ToString
public class HighloadV3Config implements WalletConfig {
    long walletId;
    int mode;
    int queryId;
    long createdAt;
    StateInit stateInit;
    Cell body;
    long timeOut;
}
