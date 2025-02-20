package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.*;

/**
 *
 *
 * <pre>
 * _ (HashmapE 32 ^(BinTree ShardDescr)) = ShardHashes;
 * </pre>
 */
@Builder
@Data
public class ShardHashes {

  TonHashMapE shardHashes;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            shardHashes.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((BinTree) v).toCell()).endCell()))
        .endCell();
  }

  public static ShardHashes deserialize(CellSlice cs) {
    return ShardHashes.builder()
        .shardHashes(
            cs.loadDictE(32, k -> k.readInt(32), v -> BinTree.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
