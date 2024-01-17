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
 * account_descr$_
 *   account:^Account
 *   last_trans_hash:bits256
 *   last_trans_lt:uint64 = ShardAccount;
 */
public class ShardAccount {
    Account account;
    BigInteger lastTransHash;
    BigInteger lastTransLt;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(account.toCell())
                .storeUint(lastTransHash, 64)
                .storeUint(lastTransLt, 64)
                .endCell();
    }
}
