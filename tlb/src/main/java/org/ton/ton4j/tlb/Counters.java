package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * counters#_
 * last_updated:uint32
 * total:uint64
 * cnt2048:uint64
 * cnt65536:uint64 = Counters;
 * </pre>
 */
@Builder
@Data
public class Counters implements Serializable {

  long lastUpdated;
  BigInteger total;
  BigInteger cnt2048;
  BigInteger cnt65536;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(lastUpdated, 32)
        .storeUint(total, 64)
        .storeUint(cnt2048, 64)
        .storeUint(cnt65536, 64)
        .endCell();
  }

  public static Counters deserialize(CellSlice cs) {

    return Counters.builder()
        .lastUpdated(cs.loadUint(32).longValue())
        .total(cs.loadUint(64))
        .cnt2048(cs.loadUint(64))
        .cnt65536(cs.loadUint(64))
        .build();
  }
}
