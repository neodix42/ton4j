package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * trans_storage$0001
 *    storage_ph:TrStoragePhase
 *   = TransactionDescr;
 *   </pre>
 */
@Builder
@Getter
@Setter
@ToString

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

    public static TransactionDescriptionStorage deserialize(CellSlice cs) {
        long magic = cs.loadUint(4).intValue();
        assert (magic == 0b0001) : "TransactionDescriptionStorage: magic not equal to 0b0001, found 0x" + Long.toHexString(magic);

        return TransactionDescriptionStorage.builder()
                .magic(0b0001)
                .storagePhase(StoragePhase.deserialize(cs))
                .build();
    }
}
