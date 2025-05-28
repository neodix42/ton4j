package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.smartcontract.SendMode;

@Builder
@Data
public class MultiSigV2SendMessageAction implements MultiSigV2Action {
  @Deprecated(since = "0.9.9", forRemoval = true)
  int mode;

  SendMode sendMode;
  Cell message;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xf1381e5bL, 32)
        .storeUint(mode, 8)
        .storeRef(message)
        .endCell();
  }
}
