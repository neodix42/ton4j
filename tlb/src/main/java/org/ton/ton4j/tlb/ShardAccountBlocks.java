package org.ton.ton4j.tlb;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapAugE;

/**
 *
 *
 * <pre>
 * _ (HashmapAugE 256 AccountBlock CurrencyCollection) = ShardAccountBlocks;
 * </pre>
 */
@Builder
@Data
public class ShardAccountBlocks {
  TonHashMapAugE shardAccountBlocks;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(
            shardAccountBlocks.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((AccountBlock) v).toCell()).endCell(),
                e -> CellBuilder.beginCell().storeCell(((CurrencyCollection) e).toCell()).endCell(),
                (fk, fv) -> CellBuilder.beginCell().storeUint(((Long) fk) + ((Long) fv), 32)))
        .endCell();
  }

  public static ShardAccountBlocks deserialize(CellSlice cs) {
    return ShardAccountBlocks.builder()
        .shardAccountBlocks(
            cs.loadDictAugE(
                256,
                k -> k.readUint(256),
                AccountBlock::deserialize,
                //                v -> v,
                CurrencyCollection::deserialize)) // CurrencyCollection::deserialize))
        .build();
  }

  public List<ShardAccount> getShardAccountBlocksAsList() {
    List<ShardAccount> shardAccounts = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry :
        this.shardAccountBlocks.elements.entrySet()) {
      shardAccounts.add((ShardAccount) entry.getValue().getLeft());
    }
    return shardAccounts;
  }
}
