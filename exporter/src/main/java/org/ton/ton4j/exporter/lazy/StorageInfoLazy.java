package org.ton.ton4j.exporter.lazy;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

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
public class StorageInfoLazy implements Serializable {
  StorageUsedLazy storageUsed;
  StorageExtraInfoLazy storageExtraInfo;
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

  public static StorageInfoLazy deserialize(CellSliceLazy cs) {
    return StorageInfoLazy.builder()
        .storageUsed(StorageUsedLazy.deserialize(cs))
        .storageExtraInfo(StorageExtraInfoLazy.deserialize(cs))
        .lastPaid(cs.loadUint(32).longValue())
        .duePayment(cs.loadBit() ? cs.loadCoins() : null)
        .build();
  }
}
