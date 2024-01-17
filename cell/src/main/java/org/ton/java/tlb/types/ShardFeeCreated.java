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
/**
 *_ fees:CurrencyCollection create:CurrencyCollection = ShardFeeCreated;
 */
public class ShardFeeCreated {

    CurrencyCollection fees;
    CurrencyCollection create;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(fees.toCell())
                .storeCell(create.toCell())
                .endCell();
    }

    public static ShardFeeCreated deserialize(CellSlice cs) {
        return ShardFeeCreated.builder()
                .fees(CurrencyCollection.deserialize(cs))
                .create(CurrencyCollection.deserialize(cs))
                .build();
    }
}
