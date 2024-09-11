package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

/**
 * <pre>
 * ^[
 *   in_msg:(Maybe ^(Message Any))
 *   out_msgs:(HashmapE 15 ^(Message Any))
 *  ]
 *  </pre>
 */
@Builder
@Data
public class TransactionIO {
    Message in;
    TonHashMapE out;

    public Cell toCell() {

        Cell dictCell = out.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 15).endCell().getBits(),
                v -> CellBuilder.beginCell().storeRef((Cell) v).endCell()
        );
        return CellBuilder.beginCell()
                .storeRefMaybe(in.toCell())
                .storeDict(dictCell)
                .endCell();
    }

    public static TransactionIO deserialize(CellSlice cs) {
        return null;
    }
}
