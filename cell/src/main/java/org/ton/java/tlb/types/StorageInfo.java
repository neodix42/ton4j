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
/**
 * storage_info$_
 *   used:StorageUsed
 *   last_paid:uint32
 *   due_payment:(Maybe Grams) = StorageInfo;
 */
public class StorageInfo {
    StorageUsed storageUsed;
    long lastPaid;
    BigInteger duePayment;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(storageUsed.toCell())
                .storeUint(lastPaid, 32)
                .storeCoinsMaybe(duePayment)
                .endCell();
    }

    public static StorageInfo deserialize(CellSlice cs) {
        StorageUsed storageUsed = StorageUsed.deserialize(cs);
        long lastPaid = cs.loadUint(32).longValue();
        boolean isDuePayment = cs.loadBit();
        return StorageInfo.builder()
                .storageUsed(storageUsed)
                .lastPaid(lastPaid)
                .duePayment(isDuePayment ? cs.loadUint(64) : null)
                .build();
    }
}
