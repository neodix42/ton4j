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
 * trans_merge_install$0111
 *   split_info:SplitMergeInfo
 *   prepare_transaction:^Transaction
 *   storage_ph:(Maybe TrStoragePhase)
 *   credit_ph:(Maybe TrCreditPhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool destroyed:Bool
 *   = TransactionDescr;
 *   </pre>
 */
@Builder
@Data
public class TransactionDescriptionMergeInstall implements TransactionDescription {
  int magic;
  SplitMergeInfo splitInfo;
  Transaction prepareTransaction;
  StoragePhase storagePhase;
  CreditPhase creditPhase;
  ComputePhase computePhase;
  ActionPhase actionPhase;
  boolean aborted;
  boolean destroyed;

  private String getMagic() {
    return Long.toBinaryString(magic);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0b0111, 4)
        .storeCell(splitInfo.toCell())
        .storeRef(prepareTransaction.toCell())
        .storeCellMaybe(storagePhase.toCell())
        .storeCellMaybe(creditPhase.toCell())
        .storeCell(computePhase.toCell())
        .storeRefMaybe(actionPhase.toCell())
        .storeBit(aborted)
        .storeBit(destroyed)
        .endCell();
  }

  public static TransactionDescriptionMergeInstall deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).intValue();
    assert (magic == 0b0111)
        : "TransactionDescriptionMergeInstall: magic not equal to 0b0111, found 0x"
            + Long.toHexString(magic);

    return TransactionDescriptionMergeInstall.builder()
        .magic(0b0111)
        .splitInfo(SplitMergeInfo.deserialize(cs))
        .prepareTransaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
        .storagePhase(cs.loadBit() ? StoragePhase.deserialize(cs) : null)
        .creditPhase(cs.loadBit() ? CreditPhase.deserialize(cs) : null)
        .computePhase(ComputePhase.deserialize(cs))
        .actionPhase(
            cs.loadBit() ? ActionPhase.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
        .aborted(cs.loadBit())
        .destroyed(cs.loadBit())
        .build();
  }
}
