package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * storage_info$_
 * used:StorageUsed
 * storage_extra:StorageExtraInfo
 * last_paid:uint32
 * due_payment:(Maybe Grams) = StorageInfo;
 * </pre>
 */
@Builder
@Data
public class StorageInfo implements Serializable {
  StorageUsed storageUsed;
  StorageExtraInfo storageExtraInfo;
  long lastPaid;
  BigInteger duePayment;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(storageUsed.toCell())
        .storeCell(storageExtraInfo.toCell())
        .storeUint(lastPaid, 32)
        .storeCoinsMaybe(duePayment)
        .endCell();
  }

  public static StorageInfo deserialize(CellSlice cs) {
    return StorageInfo.builder()
        .storageUsed(StorageUsed.deserialize(cs))
        .storageExtraInfo(StorageExtraInfo.deserialize(cs))
        .lastPaid(cs.loadUint(32).longValue())
        .duePayment(cs.loadBit() ? cs.loadCoins() : null)
        .build();
  }
}
