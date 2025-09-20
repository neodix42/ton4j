package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * _ (HashmapE 32 ^(BinTree ShardDescr)) = ShardHashes;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class ShardHashes implements Serializable {

  TonHashMapE shardHashes;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            shardHashes.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 32).endCell().getBits(),
                v -> CellBuilder.beginCell().storeRef(((BinTree) v).toCell()).endCell()))
        .endCell();
  }

  public static ShardHashes deserialize(CellSlice cs) {
    return ShardHashes.builder()
        .shardHashes(
            cs.loadDictE(
                32,
                k -> k.readUint(32),
                v -> BinTree.deserialize(CellSlice.beginParse(CellSlice.beginParse(v).loadRef()))))
        .build();
  }
}
