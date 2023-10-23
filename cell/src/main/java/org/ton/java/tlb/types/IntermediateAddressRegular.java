package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

/**
 * interm_addr_regular$0 use_dest_bits:(#<= 96) = IntermediateAddress;
 */
@Builder
@Getter
@Setter
@ToString
public class IntermediateAddressRegular implements IntermediateAddress {
    int use_dest_bits; // 7 bits

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeBit(false)
                .storeUint(use_dest_bits, 7)
                .endCell();
    }
}
