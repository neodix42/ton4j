package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class AccountStateFrozen implements AccountState {
    int magic;
    BigInteger stateHash;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b01, 2)
                .storeUint(stateHash, 256)
                .endCell();
    }

    public static AccountStateFrozen deserialize(CellSlice cs) {
        return null;
    }
}
