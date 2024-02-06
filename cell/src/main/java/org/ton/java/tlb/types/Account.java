package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

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

    public static Account deserialize(CellSlice cs) {
        boolean isAccount = cs.loadBit();
        if (!isAccount) {
            return Account.builder().isNone(true).build();

        }
        MsgAddressInt address = MsgAddressInt.deserialize(cs);
        StorageInfo info = StorageInfo.deserialize(cs);
        AccountStorage storage = AccountStorage.deserialize(cs);

        Account account = Account.builder()
                .isNone(false)
                .address(address)
                .storageInfo(info)
                .accountStorage(storage)
                .build();
        System.out.println("account " + account);
        return account;
    }
}
