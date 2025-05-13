package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * account_storage$_
 *     last_trans_lt:uint64
 *     balance:CurrencyCollection
 *     state:AccountState
 *   = AccountStorage;
 *   </pre>
 */
@Builder
@Data
public class AccountStorage implements Serializable {
  String accountStatus;
  BigInteger lastTransactionLt;
  CurrencyCollection balance;
  AccountState accountState;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeUint(lastTransactionLt, 64)
        .storeCell(balance.toCell())
        .storeCell(accountState.toCell())
        .endCell();
  }

  public static AccountStorage deserialize(CellSlice cs) {
    AccountStorage accountStorage = AccountStorage.builder().build();

    BigInteger lastTransactionLt = cs.loadUint(64);
    CurrencyCollection coins = CurrencyCollection.deserialize(cs);

    boolean isStatusActive = cs.preloadBit();
    if (isStatusActive) {
      accountStorage.setAccountStatus("ACTIVE");
      accountStorage.setAccountState(AccountStateActive.deserialize(cs));
    } else {

      boolean isStatusFrozen = cs.preloadBitAt(2);
      if (isStatusFrozen) {
        accountStorage.setAccountStatus("FROZEN");
        accountStorage.setAccountState(AccountStateFrozen.deserialize(cs));
      } else {
        accountStorage.setAccountStatus("UNINIT");
        accountStorage.setAccountState(AccountStateUninit.deserialize(cs));
      }
    }
    accountStorage.setLastTransactionLt(lastTransactionLt);
    accountStorage.setBalance(coins);
    return accountStorage;
  }
}
