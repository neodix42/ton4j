package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

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
  BigInteger lastTransHash;
  BigInteger lastTransLt;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeRef(account.toCell())
        .storeUint(lastTransHash, 256)
        .storeUint(lastTransLt, 64)
        .endCell();
  }

  public static ShardAccount deserialize(CellSlice cs) {
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
