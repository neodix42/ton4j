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
 * trans_storage$0001
 *    storage_ph:TrStoragePhase
 *   = TransactionDescr;
 */
public class TransactionDescriptionStorage {
    int magic;
    StoragePhase storagePhase;

    private String getMagic() {
        return Long.toBinaryString(magic);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b0001, 4)
                .storeCell(storagePhase.toCell())
                .endCell();
    }
}
