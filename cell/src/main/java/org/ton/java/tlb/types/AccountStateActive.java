package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class AccountStateActive implements AccountState {
    int magic;
    StateInit stateInit;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(1, 1)
                .storeCell(stateInit.toCell())
                .endCell();
    }

    public static AccountStateActive deserialize(CellSlice cs) {
        return AccountStateActive.builder()
                .stateInit(StateInit.deserialize(cs))
                .build();
    }
}
