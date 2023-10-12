package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

@Builder
@Getter
@Setter
@ToString
/**
 * value_flow#b8e48dfb
 *  ^[
 *   from_prev_blk:CurrencyCollection
 *   to_next_blk:CurrencyCollection
 *   imported:CurrencyCollection
 *   exported:CurrencyCollection ]
 *
 *   fees_collected:CurrencyCollection
 *
 *   ^[
 *   fees_imported:CurrencyCollection
 *   recovered:CurrencyCollection
 *   created:CurrencyCollection
 *   minted:CurrencyCollection
 *   ] = ValueFlow;
 */
//todo impl in tlb load
public class ValueFlow {
    long magic;
    Cell c1;
    CurrencyCollection feesCollected;
    Cell c2;

    private String getMagic() {
        return Long.toHexString(magic);
    }
}
