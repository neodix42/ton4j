package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import static java.util.Objects.isNull;

@Builder
@Data
public class Account implements Serializable {
  boolean isNone;
  MsgAddressInt address;
  StorageInfo storageInfo;
  AccountStorage accountStorage;

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

  public static Account deserialize(CellSlice cs) {
    boolean isAccount = cs.loadBit();
    if (!isAccount) {
      return Account.builder().isNone(true).build();
    }

    return Account.builder()
        .isNone(false)
        .address(MsgAddressInt.deserialize(cs))
        .storageInfo(StorageInfo.deserialize(cs))
        .accountStorage(AccountStorage.deserialize(cs))
        .build();
  }

  public StateInit getStateInit() {
    if (isNull(accountStorage)) {
      return null;
    }
    return ((AccountStateActive) accountStorage.getAccountState()).getStateInit();
  }

  public BigInteger getBalance() {
    if (isNull(accountStorage)) {
      return null;
    }
    return accountStorage.getBalance().getCoins();
  }

  public String getAccountState() {
    if (isNull(accountStorage) || isNull(accountStorage.getAccountState())) {
      return "uninitialized";
    }
    if (accountStorage.getAccountState() instanceof AccountStateActive) {
      return "active";
    }
    if (accountStorage.getAccountState() instanceof AccountStateFrozen) {
      return "frozen";
    }
    if (accountStorage.getAccountState() instanceof AccountStateUninit) {
      return "uninitialized";
    } else {
      return "unknown";
    }
  }
}
