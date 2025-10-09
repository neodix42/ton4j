package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.*;
import org.ton.ton4j.tlb.DepthBalanceInfo;
import org.ton.ton4j.tlb.ShardAccount;

/**
 * The key here is the account address
 *
 * <pre>
 * _ (HashmapAugE 256 ShardAccount DepthBalanceInfo) = ShardAccounts;
 * </pre>
 */
@Slf4j
@Builder
@Data
public class ShardAccountsLazy {
  TonHashMapAugELazy shardAccounts;

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

  public static ShardAccountsLazy deserialize(CellSliceLazy cs) {

    return ShardAccountsLazy.builder()
        .shardAccounts(
            cs.loadDictAugE(256, k -> k.readUint(256), v -> v, e -> e)
            //                ShardAccountLazy::deserialize,
            //                DepthBalanceInfoLazy::deserialize)
            )
        .build();
  }

  public List<ShardAccountLazy> getShardAccountsAsList() {
    List<ShardAccountLazy> shardAccounts = new ArrayList<>();
    for (Map.Entry<Object, ValueExtra> entry : this.shardAccounts.elements.entrySet()) {
      shardAccounts.add((ShardAccountLazy) entry.getValue().getValue());
    }
    return shardAccounts;
  }

  public ShardAccountLazy getShardAccountByAddress(Address address) {
    log.info("searching among {} shard accounts", this.shardAccounts.elements.size());
    for (Map.Entry<Object, ValueExtra> entry : this.shardAccounts.elements.entrySet()) {
      CellSliceLazy cs = (CellSliceLazy) entry.getValue().getValue();
      log.info("key {} value {}", entry.getKey(), (ShardAccountLazy.deserialize(cs)));
    }
    ValueExtra valueExtra = this.shardAccounts.elements.get(address.toBigInteger());

    CellSliceLazy cs = (CellSliceLazy) valueExtra.getValue();
    return (ShardAccountLazy.deserialize(cs));
    // or
    //    return (ShardAccountLazy) valueExtra.getValue();
  }
}
