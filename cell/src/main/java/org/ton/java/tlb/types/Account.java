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
public class Account {
    boolean isNone;
    MsgAddressInt address;
    StorageInfo storageInfo;
    AccountStorage accountStorage;

    public Cell toCell() {
        if (isNone) {
            return CellBuilder.beginCell()
                    .storeBit(true)
                    .endCell();
        } else {
            return CellBuilder.beginCell()
                    .storeBit(false)
                    .storeCell(address.toCell())
                    .storeCell(storageInfo.toCell())
                    .storeCell(accountStorage.toCell())
                    .endCell();
        }
    }
}
