package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.CellType;

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
public class ShardAccount implements Serializable {
  Account account;
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

  public static ShardAccount deserialize(CellSlice cs) {
    if (cs.type == CellType.PRUNED_BRANCH) {
      return null;
    }
    if (cs.getRefsCount() == 0) {
      return null;
    }
    return ShardAccount.builder()
        .account(Account.deserialize(CellSlice.beginParse(cs.loadRef())))
        .lastTransHash(cs.loadUint(256))
        .lastTransLt(cs.loadUint(64))
        .build();
  }

  public BigInteger getBalance() {
    return account.getAccountStorage().getBalance().getCoins();
  }
}
