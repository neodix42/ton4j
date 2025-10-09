package org.ton.ton4j.exporter.lazy;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellType;
import org.ton.ton4j.utils.Utils;

/**
 * </pre>
 * account_descr$_
 * account:^Account
 * last_trans_hash:bits256
 * last_trans_lt:uint64 = ShardAccount;
 * <pre>
 */
@Builder
@Data
public class ShardAccountLazy implements Serializable {
  AccountLazy account;
  public BigInteger lastTransHash;
  BigInteger lastTransLt;

  public String getLastTransHash() {
    return lastTransHash.toString(16);
  }

  public Cell toCell() {
    if (isNull(account)) {
      return CellBuilder.beginCell().endCell();
    }
    return CellBuilder.beginCell()
        .storeRef(account.toCell())
        .storeUint(lastTransHash, 256)
        .storeUint(lastTransLt, 64)
        .endCell();
  }

  public static ShardAccountLazy deserialize(CellSliceLazy cs) {
    if (cs.type == CellType.PRUNED_BRANCH) {
      return null;
    }
    //    if (cs.getRefsCount() == 0) {
    //      return null;
    //    }
    if (cs.getHashes().length == 0) {
      return null;
    }
    //    byte[] hash = Utils.slice(cs.getHashes(), 32, 32);
    byte[] hash = Utils.slice(cs.getHashes(), cs.getHashes().length - 32, 32);
    // cs.hashes = Arrays.copyOfRange(cs.getHashes(), 32, cs.getHashes().length);
    Cell accountCell = cs.getRefByHash(hash);
    return ShardAccountLazy.builder()
        .account(AccountLazy.deserialize(CellSliceLazy.beginParse(cs.cellDbReader, accountCell)))
        .lastTransHash(cs.loadUint(256))
        .lastTransLt(cs.loadUint(64))
        .build();
  }

  public BigInteger getBalance() {
    return account.getAccountStorage().getBalance().getCoins();
  }
}
