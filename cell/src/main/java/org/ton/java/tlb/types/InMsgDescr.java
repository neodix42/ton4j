package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapAugE;

@Builder
@Getter
@Setter
@ToString
/**
 * _ (HashmapAugE 256 InMsg ImportFees) = InMsgDescr;
 */

public class InMsgDescr {
    TonHashMapAugE inMsg;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeDict(inMsg.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).endCell().bits,
                        v -> CellBuilder.beginCell().storeCell(((InMsg) v).toCell()),
                        e -> CellBuilder.beginCell().storeCell(((ImportFees) e).toCell()),
                        (fk, fv) -> CellBuilder.beginCell().storeUint(0, 1) // todo
                )).endCell();
    }

    public static InMsgDescr deserialize(CellSlice cs) {
        return InMsgDescr.builder()
                .inMsg(cs.loadDictAugE(256,
                        k -> k.readInt(256),
                        InMsg::deserialize,
                        ImportFees::deserialize))
                .build();
    }

    public long getCount() {
        return inMsg.elements.size();
    }
}
