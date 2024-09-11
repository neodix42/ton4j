package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
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
public class AccountStorage {
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

        boolean isStatusActive = cs.loadBit();
        if (isStatusActive) {
            accountStorage.setAccountStatus("ACTIVE");
            accountStorage.setAccountState(AccountStateActive.deserialize(cs));
        } else {
            boolean isStatusFrozen = cs.loadBit();
            if (isStatusFrozen) {
                accountStorage.setAccountStatus("FROZEN");
                if (cs.getRestBits() != 0) {
                    BigInteger stateHash = cs.loadUint(256);
                    accountStorage.setAccountState(
                            AccountStateFrozen.builder()
                                    .stateHash(stateHash)
                                    .build());
                }
            } else {
                accountStorage.setAccountStatus("UNINIT");
                accountStorage.setAccountState(
                        AccountStateUninit.builder().build());
            }
        }
        accountStorage.setLastTransactionLt(lastTransactionLt);
        accountStorage.setBalance(coins);
        return accountStorage;
    }
}
