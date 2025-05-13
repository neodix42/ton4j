package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * trans_split_prepare$0100
 *   split_info:SplitMergeInfo
 *   storage_ph:(Maybe TrStoragePhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool destroyed:Bool
 *   = TransactionDescr;
 *   </pre>
 */
@Builder
@Data
public class TransactionDescriptionSplitPrepare implements TransactionDescription, Serializable {
  int magic;
  SplitMergeInfo splitInfo;
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
        .storeUint(0b0100, 4)
        .storeCell(splitInfo.toCell())
        .storeCellMaybe(storagePhase.toCell())
        .storeCell(computePhase.toCell())
        .storeRefMaybe(actionPhase.toCell())
        .storeBit(aborted)
        .storeBit(destroyed)
        .endCell();
  }

  public static TransactionDescriptionSplitPrepare deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).intValue();
    assert (magic == 0b0100)
        : "TransactionDescriptionSplitPrepare: magic not equal to 0b0100, found 0x"
            + Long.toHexString(magic);
    return TransactionDescriptionSplitPrepare.builder()
        .magic(0b0100)
        .splitInfo(SplitMergeInfo.deserialize(cs))
        .storagePhase(cs.loadBit() ? StoragePhase.deserialize(cs) : null)
        .computePhase(ComputePhase.deserialize(cs))
        .actionPhase(
            cs.loadBit() ? ActionPhase.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
        .aborted(cs.loadBit())
        .destroyed(cs.loadBit())
        .build();
  }

  @Override
  public String getType() {
    return "split-prepare";
  }
}
