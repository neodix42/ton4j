package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;

@Builder
@Getter
@Setter
@ToString
public class AnyMessage {
    Cell payload;       // *cell.Cell
    Address senderAddr; // address.Address
    Address destAddr;   // address.Address

    // todo implement get src/dst address by msgType
}
