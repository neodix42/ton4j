package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;
import org.ton.ton4j.tlb.*;

@Builder
@Data
public class HighloadV3Config implements WalletConfig {
  long walletId;

  @Deprecated(since = "0.9.9", forRemoval = true)
  int mode;

  SendMode sendMode;
  int queryId;
  long createdAt;
  StateInit stateInit;
  Cell body;
  long timeOut;
}
