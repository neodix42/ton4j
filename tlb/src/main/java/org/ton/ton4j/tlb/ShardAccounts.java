package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.*;

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
    if (isNull(shardAccounts)) {
      return CellBuilder.beginCell().endCell();
    }
    return CellBuilder.beginCell()
        .storeCell(
            shardAccounts.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 256).endCell().getBits(),
                v -> {
                  if (isNull(v)) {
                    return null;
                  }
                  return CellBuilder.beginCell().storeCell(((ShardAccount) v).toCell()).endCell();
                },
                e -> {
                  if (isNull(e)) {
                    return null;
                  }
                  return CellBuilder.beginCell()
                      .storeCell(((DepthBalanceInfo) e).toCell())
                      .endCell();
                },
                (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1)))
        .endCell();
  }

  public static ShardAccounts deserialize(CellSlice cs) {

    if (cs.isExotic()) {
      return ShardAccounts.builder().build();
    }

    return ShardAccounts.builder()
        .shardAccounts(
            cs.loadDictAugE(
                256,
                k -> k.readUint(256),
                ShardAccount::deserialize,
                DepthBalanceInfo::deserialize))
        .build();
  }

  public List<ShardAccount> getShardAccountsAsList() {
    List<ShardAccount> shardAccounts = new ArrayList<>();
    for (Map.Entry<Object, ValueExtra> entry : this.shardAccounts.elements.entrySet()) {
      shardAccounts.add((ShardAccount) entry.getValue().getValue());
    }
    return shardAccounts;
  }
}
