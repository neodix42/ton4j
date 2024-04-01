package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;


public interface TransactionDescription {

    Cell toCell();

    static TransactionDescription deserialize(CellSlice cs) {
        int pfx = cs.preloadUint(3).intValue();
        switch (pfx) {
            case 0b000 -> {
                boolean isStorage = cs.preloadBit();
                if (isStorage) {
                    return TransactionDescriptionStorage.deserialize(cs);
                    //return TransactionDescription.builder().description(desc).build();
                }
                return TransactionDescriptionOrdinary.deserialize(cs); // skipped was true
//                return TransactionDescription.builder().description(descOrdinary).build();
            }
            case 0b001 -> {
                return TransactionDescriptionTickTock.deserialize(cs); // skipped was true
//                return TransactionDescription.builder().description(descTickTock).build();
            }
            case 0b010 -> {
                boolean isInstall = cs.preloadBit();
                if (isInstall) {
                    return TransactionDescriptionSplitInstall.deserialize(cs); // skipped was true
//                    return TransactionDescription.builder().description(descSplit).build();
                }
                return TransactionDescriptionSplitPrepare.deserialize(cs); // skipped was true
//                return TransactionDescription.builder().description(descSplitPrepare).build();
            }
            case 0b011 -> {
                boolean isInstall = cs.preloadBit();
                if (isInstall) {
                    return TransactionDescriptionMergeInstall.deserialize(cs); // skipped was true
//                    return TransactionDescription.builder().description(descMerge).build();
                }
                return TransactionDescriptionMergePrepare.deserialize(cs); // skipped was true
//                return TransactionDescription.builder().description(descMergePrepare).build();
            }
        }
        throw new Error("unknown transaction description type (must be in range [0..3], found 0x" + Integer.toBinaryString(pfx));
    }
}
