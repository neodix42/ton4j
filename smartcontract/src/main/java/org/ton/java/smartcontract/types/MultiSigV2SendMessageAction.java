package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Data
public class MultiSigV2SendMessageAction implements MultiSigV2Action {
  int mode;
  Cell message;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xf1381e5bL, 32)
        .storeUint(mode, 8)
        .storeRef(message)
        .endCell();
  }
}
