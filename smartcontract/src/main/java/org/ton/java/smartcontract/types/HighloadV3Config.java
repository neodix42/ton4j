package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

@Builder
@Getter
@Setter
@ToString
public class HighloadV3Config implements WalletConfig {
    long walletId;
    int mode;
    int queryId;
    long createdAt;
    Cell body;
    long timeOut;
}
