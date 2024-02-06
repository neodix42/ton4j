package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

import static java.util.Objects.isNull;

@Builder
@Getter
@Setter
@ToString
/**
 * _ (HashmapAugE 32 KeyExtBlkRef KeyMaxLt) = OldMcBlocksInfo;
 */
public class OldMcBlocksInfo {
    TonHashMapAugE list;


    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeDict(list.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 32).bits,
                        v -> CellBuilder.beginCell().storeCell(((KeyExtBlkRef) v).toCell()),
                        e -> CellBuilder.beginCell().storeCell(((KeyMaxLt) e).toCell()),
                        (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                ))
                .endCell();
    }

    public static OldMcBlocksInfo deserialize(CellSlice cs) {
        if (isNull(cs)) {
            return OldMcBlocksInfo.builder().build();
        }
        return OldMcBlocksInfo.builder()
                .list(cs.loadDictAugE(32,
                        k -> k.readInt(32),
                        v -> KeyExtBlkRef.deserialize(CellSlice.beginParse(v)),
                        e -> KeyMaxLt.deserialize(CellSlice.beginParse(e))))
                .build();
    }
}
