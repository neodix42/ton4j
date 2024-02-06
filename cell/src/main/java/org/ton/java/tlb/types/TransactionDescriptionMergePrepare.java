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
/**
 * trans_split_prepare$0100
 *   split_info:SplitMergeInfo
 *   storage_ph:(Maybe TrStoragePhase)
 *   compute_ph:TrComputePhase
 *   action:(Maybe ^TrActionPhase)
 *   aborted:Bool destroyed:Bool
 *   = TransactionDescr;
 */
public class TransactionDescriptionMergePrepare {
    int magic;
    SplitMergeInfo splitInfo;
    StoragePhase storagePhase;
    boolean aborted;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b0100, 4)
                .storeCell(splitInfo.toCell())
                .storeCellMaybe(storagePhase.toCell())
                .storeBit(aborted)
                .endCell();
    }

    public static TransactionDescriptionMergePrepare deserialize(CellSlice cs) {
        long magic = cs.loadUint(4).intValue();
        assert (magic == 0b0110) : "TransactionDescriptionMergePrepare: magic not equal to 0b0110, found 0x" + Long.toHexString(magic);

        return TransactionDescriptionMergePrepare.builder()
                .magic(0b0110)
                .splitInfo(SplitMergeInfo.deserialize(cs))
                .storagePhase(StoragePhase.deserialize(cs))
                .aborted(cs.loadBit())
                .build();
    }
}
