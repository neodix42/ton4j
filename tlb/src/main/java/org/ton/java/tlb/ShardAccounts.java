package org.ton.java.tlb;

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
 * _ (HashmapAugE 256 ShardAccount DepthBalanceInfo) = ShardAccounts;
 * </pre>
 */
@Builder
@Data
public class ShardAccounts {
  TonHashMapAugE shardAccounts;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(
            shardAccounts.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((ShardAccount) v).toCell()),
                e -> CellBuilder.beginCell().storeCell(((DepthBalanceInfo) e).toCell()),
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1)))
        .endCell();
  }

  public static ShardAccounts deserialize(CellSlice cs) {
    return ShardAccounts.builder()
        .shardAccounts(
            CellSlice.beginParse(cs)
                .loadDictAugE(
                    256,
                    k -> k.readInt(256),
                    v -> ShardAccount.deserialize(v),
                    e -> DepthBalanceInfo.deserialize(e)))
        .build();
  }

  public List<ShardAccount> getShardAccountsAsList() {
    List<ShardAccount> shardAccounts = new ArrayList<>();
    for (Map.Entry<Object, Pair<Object, Object>> entry : this.shardAccounts.elements.entrySet()) {
      shardAccounts.add((ShardAccount) entry.getValue().getLeft());
    }
    return shardAccounts;
  }
}
