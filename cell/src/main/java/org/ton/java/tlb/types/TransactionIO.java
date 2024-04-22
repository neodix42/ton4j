package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
/**
 * ^[
 *   in_msg:(Maybe ^(Message Any))
 *   out_msgs:(HashmapE 15 ^(Message Any))
 *  ]
 */
public class TransactionIO {
    Message in;
    TonHashMapE out;

    public Cell toCell() {

        Cell dictCell = out.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 15).endCell().bits,
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
