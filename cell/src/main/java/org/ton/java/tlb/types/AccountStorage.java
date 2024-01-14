package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * account_storage$_
 *     last_trans_lt:uint64
 *     balance:CurrencyCollection
 *     state:AccountState
 *   = AccountStorage;
 */
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
}
