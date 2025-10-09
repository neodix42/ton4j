package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

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
public class AnycastLazy implements Serializable {
  int depth;
  int rewritePfx;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(depth, 5).storeUint(rewritePfx, depth).endCell();
  }

  public static AnycastLazy deserialize(CellSliceLazy cs) {
    int depth = cs.loadUint(5).intValue();
    return AnycastLazy.builder().depth(depth).rewritePfx(cs.loadUint(depth).intValue()).build();
  }
}
