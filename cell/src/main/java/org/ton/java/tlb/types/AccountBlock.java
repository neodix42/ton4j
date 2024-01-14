package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapAugE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * acc_trans#5
 *  account_addr:bits256
 *  transactions:(HashmapAug 64 ^Transaction CurrencyCollection)
 *  state_update:^(HASH_UPDATE Account)
 *  = AccountBlock;
 */
public class AccountBlock {
    long magic;
    BigInteger addr;
    TonHashMapAugE transactions;
    Cell stateUpdate;

    public Cell toCell() {
        Cell dictCell = transactions.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 64).bits,
                v -> CellBuilder.beginCell().storeRef(((Transaction) v).toCell()),
                e -> CellBuilder.beginCell().storeCell(((CurrencyCollection) e).toCell())
        );
        return CellBuilder.beginCell()
                .storeUint(0x5, 32)
                .storeUint(addr, 256)
                .storeDict(dictCell)
                .storeRef(stateUpdate)
                .endCell();
    }
}
