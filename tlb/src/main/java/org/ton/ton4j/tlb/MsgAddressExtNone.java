package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * addr_none$00 = MsgAddressExt;
 * </pre>
 */
@Builder
@Data
public class MsgAddressExtNone implements MsgAddressExt, Serializable {
  int magic;

  @Override
  public String toString() {
    return "";
  }

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(0, 2).endCell();
  }

  public static MsgAddressExtNone deserialize(CellSlice cs) {
    int magic = cs.loadUint(2).intValue();
    assert (magic == 0b00) : "MsgAddressExtNone: magic not equal to 0b00, found " + magic;

    return MsgAddressExtNone.builder().magic(magic).build();
  }
}
