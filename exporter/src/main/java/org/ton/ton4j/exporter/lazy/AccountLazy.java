package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.tlb.*;

/**
 *
 *
 * <pre>
 * account_none$0 = Account;
 * account$1 addr:MsgAddressInt storage_stat:StorageInfo storage:AccountStorage = Account;
 * </pre>
 */
@Builder
@Data
public class AccountLazy implements Serializable {
  boolean isNone;
  MsgAddressIntLazy address;
  StorageInfoLazy storageInfo;
  AccountStorageLazy accountStorage;

  public Cell toCell() {
    if (isNone) {
      return CellBuilder.beginCell().storeBit(false).endCell();
    } else {
      return CellBuilder.beginCell()
          .storeBit(true)
          .storeCell(address.toCell())
          .storeCell(storageInfo.toCell())
          .storeCell(accountStorage.toCell())
          .endCell();
    }
  }

  public static AccountLazy deserialize(CellSliceLazy cs) {

    //    if (cs.isExotic()) {
    //      AccountLazy.builder().isNone(true).build();
    //    }

    boolean isAccount = cs.loadBit();
    if (!isAccount) {
      return AccountLazy.builder().isNone(true).build();
    }

    return AccountLazy.builder()
        .isNone(false)
        .address(MsgAddressIntLazy.deserialize(cs))
        .storageInfo(StorageInfoLazy.deserialize(cs))
        .accountStorage(AccountStorageLazy.deserialize(cs))
        .build();
  }
}
