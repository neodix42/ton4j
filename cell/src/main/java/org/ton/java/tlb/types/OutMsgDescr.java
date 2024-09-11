package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

/**
 * <pre>
 * _ (HashmapAugE 256 OutMsg CurrencyCollection) = OutMsgDescr;
 * </pre>
 */

@Builder
@Data
public class OutMsgDescr {
    TonHashMapAugE outMsg;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeDict(outMsg.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().getBits(),
                        v -> CellBuilder.beginCell().storeCell(((OutMsg) v).toCell()),
                        e -> CellBuilder.beginCell().storeCell(((CurrencyCollection) e).toCell()),
                        (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                )).endCell();
    }

    public static OutMsgDescr deserialize(CellSlice cs) {
        return OutMsgDescr.builder()
                .outMsg(cs.loadDictAugE(256,
                        k -> k.readInt(256),
                        v -> OutMsg.deserialize(v),
                        e -> CurrencyCollection.deserialize(e)))
                .build();
    }

    public long getCount() {
        return outMsg.elements.size();
    }
}
