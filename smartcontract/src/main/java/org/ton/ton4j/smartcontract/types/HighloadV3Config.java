package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.tlb.*;

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
