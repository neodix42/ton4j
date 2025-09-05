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
 * split_state#5f327da5
 * left:^ShardStateUnsplit
 * right:^ShardStateUnsplit = ShardState;
 * </pre>
 */
@Builder
@Data
public class ShardState implements Serializable {

  long magic;
  ShardStateUnsplit left;
  ShardStateUnsplit right;

  private String getMagic() {
    return Long.toHexString(magic);
  }

  public Cell toCell() {
    if (magic == 0x5f327da5L) {
      return CellBuilder.beginCell()
          .storeUint(0x5f327da5L, 32)
          .storeRef(left.toCell())
          .storeRef(right.toCell())
          .endCell();
    }
    if (magic == 0x9023afe2L) {
      return CellBuilder.beginCell().storeUint(2418257890L, 32).storeCell(left.toCell()).endCell();
    } else {
      throw new Error("wrong magic number " + BigInteger.valueOf(magic).toString(16));
    }
  }

  public static ShardState deserialize(CellSlice cs) {
    long tag = cs.preloadUint(32).longValue();
    if (tag == 0x5f327da5L) {
      ShardStateUnsplit left, right;
      left = ShardStateUnsplit.deserialize(CellSlice.beginParse(cs.loadRef()));
      right = ShardStateUnsplit.deserialize(CellSlice.beginParse(cs.loadRef()));
      return ShardState.builder().magic(tag).left(left).right(right).build();
    } else {
      return ShardState.builder().magic(tag).left(ShardStateUnsplit.deserialize(cs)).build();
    }
  }
}
