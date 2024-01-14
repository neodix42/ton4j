package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
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

}
