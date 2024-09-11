package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Data
public class AnyMessage {
    Cell payload;       // *cell.Cell
    Address senderAddr; // address.Address
    Address destAddr;   // address.Address

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeCell(payload)
                .storeAddress(senderAddr)
                .storeAddress(destAddr)
                .endCell();
    }

    public static AnyMessage deserialize(CellSlice cs) {
        return null;
    }

}
