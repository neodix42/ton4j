package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class Account {
    boolean isNone;
    MsgAddressInt address;
    StorageInfo storageInfo;
    AccountStorage accountStorage;

    public Cell toCell() {
        if (isNone) {
            return CellBuilder.beginCell()
                    .storeBit(false)
                    .endCell();
        } else {
            return CellBuilder.beginCell()
                    .storeBit(true)
                    .storeCell(address.toCell())
                    .storeCell(storageInfo.toCell())
                    .storeCell(accountStorage.toCell())
                    .endCell();
        }
    }

    public static Account deserialize(CellSlice cs) {
        boolean isAccount = cs.loadBit();
        if (!isAccount) {
            return Account.builder().isNone(false).build();

        }
        MsgAddressInt address = MsgAddressInt.deserialize(cs);
        StorageInfo info = StorageInfo.deserialize(cs);
        AccountStorage storage = AccountStorage.deserialize(cs);

        return Account.builder()
                .isNone(true)
                .address(address)
                .storageInfo(info)
                .accountStorage(storage)
                .build();
    }
}
