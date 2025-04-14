package org.ton.java.tlb;

import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

/**
 *
 *
 * <pre>
 * _ split_depth:(Maybe (## 5))
 *   special:(Maybe TickTock)
 *   code:(Maybe ^Cell)
 *   data:(Maybe ^Cell)
 *   library:(Maybe ^Cell) = StateInit;
 *   </pre>
 */
@Builder
@Data
public class StateInit implements Serializable {
  BigInteger depth;
  TickTock tickTock;
  Cell code;
  Cell data;
  Cell lib;

  public Cell toCell() {

    CellBuilder result = CellBuilder.beginCell();

    if (nonNull(depth)) {
      result.storeBit(true);
      result.storeUint(depth, 5);
    } else {
      result.storeBit(false);
    }

    if (nonNull(tickTock)) {
      result.storeBit(true);
      result.storeCell(tickTock.toCell());
    } else {
      result.storeBit(false);
    }
    result.storeRefMaybe(code);
    result.storeRefMaybe(data);
    result.storeRefMaybe(lib);

    return result.endCell();
  }

  public static StateInit deserialize(CellSlice cs) {
    return StateInit.builder()
        .depth(cs.loadBit() ? cs.loadUint(5) : null)
        .tickTock(cs.loadBit() ? TickTock.deserialize(cs) : null)
        .code(cs.loadMaybeRefX())
        .data(cs.loadMaybeRefX())
        .lib(cs.loadMaybeRefX())
        .build();
  }

  public Address getAddress(long wc) {
    return Address.of(wc + ":" + Utils.bytesToHex(this.toCell().getHash()));
  }

  public Address getAddress() {
    return Address.of("0:" + Utils.bytesToHex(this.toCell().getHash()));
  }
}
