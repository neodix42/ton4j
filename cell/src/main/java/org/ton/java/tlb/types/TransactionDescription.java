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
public class TransactionDescription {
    Object description; // `tlb:"."`

    public Cell toCell() {
        CellBuilder c = CellBuilder.beginCell();

        if (description instanceof TransactionDescriptionStorage) {
            c.storeUint(0b0001, 3);
            c.storeSlice(CellSlice.beginParse(((TransactionDescriptionStorage) description).toCell()));
        } else if (description instanceof TransactionDescriptionOrdinary) {
            c.storeUint(0b000, 3);
            c.storeSlice(CellSlice.beginParse(((TransactionDescriptionOrdinary) description).toCell()));
        } else if (description instanceof TransactionDescriptionOrdinary) {
            c.storeUint(0b000, 3);
            c.storeSlice(CellSlice.beginParse(((TransactionDescriptionOrdinary) description).toCell()));
        }
        return c.endCell();
    }

    public static TransactionDescription deserialize(CellSlice cs) {
        int pfx = cs.preloadUint(3).intValue();
        switch (pfx) {
            case 0b000 -> {
                boolean isStorage = cs.preloadBit();
                if (isStorage) {
                    TransactionDescriptionStorage desc = TransactionDescriptionStorage.deserialize(cs);
                    return TransactionDescription.builder().description(desc).build();
                }
                TransactionDescriptionOrdinary descOrdinary = TransactionDescriptionOrdinary.deserialize(cs); // skipped was true
                return TransactionDescription.builder().description(descOrdinary).build();
            }
            case 0b001 -> {
                TransactionDescriptionTickTock descTickTock = TransactionDescriptionTickTock.deserialize(cs); // skipped was true
                return TransactionDescription.builder().description(descTickTock).build();
            }
            case 0b010 -> {
                boolean isInstall = cs.preloadBit();
                if (isInstall) {
                    TransactionDescriptionSplitInstall descSplit = TransactionDescriptionSplitInstall.deserialize(cs); // skipped was true
                    return TransactionDescription.builder().description(descSplit).build();
                }
                TransactionDescriptionSplitPrepare descSplitPrepare = TransactionDescriptionSplitPrepare.deserialize(cs); // skipped was true
                return TransactionDescription.builder().description(descSplitPrepare).build();
            }
            case 0b011 -> {
                boolean isInstall = cs.preloadBit();
                if (isInstall) {
                    TransactionDescriptionMergeInstall descMerge = TransactionDescriptionMergeInstall.deserialize(cs); // skipped was true
                    return TransactionDescription.builder().description(descMerge).build();
                }
                TransactionDescriptionMergePrepare descMergePrepare = TransactionDescriptionMergePrepare.deserialize(cs); // skipped was true
                return TransactionDescription.builder().description(descMergePrepare).build();
            }
        }
        throw new Error("unknown transaction description type (must be in range [0..3], found 0x" + Integer.toBinaryString(pfx));
    }
}
