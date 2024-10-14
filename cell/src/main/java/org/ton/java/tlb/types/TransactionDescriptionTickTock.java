package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * trans_tick_tock$001
 *   is_tock:Bool
 *   storage_ph:TrStoragePhase
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool
 *   destroyed:Bool = TransactionDescr;
 *   </pre>
 */
@Builder
@Data
public class TransactionDescriptionTickTock implements TransactionDescription {
  int magic;
  boolean isTock;
  StoragePhase storagePhase;
  ComputePhase computePhase;
  ActionPhase actionPhase;
  boolean aborted;
  boolean destroyed;

  private String getMagic() {
    return Long.toBinaryString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b001, 3)
        .storeBit(isTock)
        .storeCell(storagePhase.toCell())
        .storeCell(computePhase.toCell())
        .storeRefMaybe(actionPhase.toCell())
        .storeBit(aborted)
        .storeBit(destroyed)
        .endCell();
  }

  public static TransactionDescriptionTickTock deserialize(CellSlice cs) {
    long magic = cs.loadUint(3).intValue();
    assert (magic == 0b001)
        : "TransactionDescriptionTickTock: magic not equal to 0b001, found 0x"
            + Long.toHexString(magic);

    return TransactionDescriptionTickTock.builder()
        .magic(0b001)
        .isTock(cs.loadBit())
        .storagePhase(StoragePhase.deserialize(cs))
        .computePhase(ComputePhase.deserialize(cs))
        .actionPhase(
            cs.loadBit() ? ActionPhase.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
        .aborted(cs.loadBit())
        .destroyed(cs.loadBit())
        .build();
  }
}
