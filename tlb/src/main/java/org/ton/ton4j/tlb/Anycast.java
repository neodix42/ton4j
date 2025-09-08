package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;
import java.math.BigInteger;

/**
 *
 *
 * <pre>{@code
 * anycast_info$_
 *  depth:(#<= 30) { depth >= 1 }
 *  rewrite_pfx:(bits depth)
 *  = Anycast;
 * }</pre>
 */
@Builder
@Data
public class Anycast implements Serializable {
  int depth;
  int rewritePfx;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(depth, 5).storeUint(rewritePfx, depth).endCell();
  }

  public static Anycast deserialize(CellSlice cs) {
    int depth = cs.loadUint(5).intValue();
    return Anycast.builder().depth(depth).rewritePfx(cs.loadUint(depth).intValue()).build();
  }
}
