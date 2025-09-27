package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.*;

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

  public List<ShardDescr> getShardDescrAsList() {
    List<ShardDescr> result = new ArrayList<>();

    for (Map.Entry<Object, Object> entry : shardHashes.elements.entrySet()) {
      BinTree binTree = (BinTree) entry.getValue();
      result.addAll(binTree.toList());
    }
    return result;
  }
}
