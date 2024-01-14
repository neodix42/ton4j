package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 * trans_split_install$0101
 *   split_info:SplitMergeInfo
 *   prepare_transaction:^Transaction
 *   installed:Bool = TransactionDescr;
 */
public class TransactionDescriptionSplitInstall {
    int magic;
    SplitMergeInfo splitInfo;
    Transaction prepareTransaction;
    boolean installed;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b0101, 4)
                .storeCell(splitInfo.toCell())
                .storeRef(prepareTransaction.toCell())
                .storeBit(installed)
                .endCell();
    }
}
