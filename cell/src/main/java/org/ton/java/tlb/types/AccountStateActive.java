package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
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
}
