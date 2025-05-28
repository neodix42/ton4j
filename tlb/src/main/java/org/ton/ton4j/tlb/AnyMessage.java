package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

@Builder
@Data
public class AnyMessage implements Serializable {
  Cell payload; // *cell.Cell
  Address senderAddr; // address.Address
  Address destAddr; // address.Address

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(payload)
        .storeAddress(senderAddr)
        .storeAddress(destAddr)
        .endCell();
  }

  public static AnyMessage deserialize(CellSlice cs) {
    return null;
  }
}
