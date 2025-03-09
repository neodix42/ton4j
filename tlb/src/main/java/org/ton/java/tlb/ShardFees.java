package org.ton.java.tlb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

/**
 *
 *
 * <pre>
 * _ (HashmapAugE 96 ShardFeeCreated ShardFeeCreated) = ShardFees;
 * </pre>
 */
@Builder
@Data
public class ShardFees {

  TonHashMapAugE shardFees;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            shardFees.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 96).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((ShardFeeCreated) v).toCell()).endCell(),
                e -> CellBuilder.beginCell().storeCell(((ShardFeeCreated) e).toCell()),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
        .endCell();
  }

  public static ShardFees deserialize(CellSlice cs) {
    return ShardFees.builder()
        .shardFees(
            cs.loadDictAugE(
                96,
                k -> k.readInt(96),
                v -> ShardFeeCreated.deserialize(v),
                e -> ShardFeeCreated.deserialize(e)))
        .build();
  }

  public List<ShardFeeCreated> getShardFeesCreatedAsList() {
    List<ShardFeeCreated> shardFeesCreated = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry : shardFees.elements.entrySet()) {
      shardFeesCreated.add((ShardFeeCreated) entry.getValue().getLeft());
    }
    return shardFeesCreated;
  }
}
