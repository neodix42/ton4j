package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
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
                        k -> CellBuilder.beginCell().storeUint((Long) k, 256).bits,
                        v -> CellBuilder.beginCell().storeCell(((InMsg) v).toCell()),
                        e -> CellBuilder.beginCell().storeCell(((ImportFees) e).toCell())
                )).endCell();
    }
}
