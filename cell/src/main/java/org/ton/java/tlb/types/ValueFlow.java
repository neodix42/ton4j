package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
public class ValueFlow {
    long magic;
    CurrencyCollection fromPrevBlk;
    CurrencyCollection toNextBlk;
    CurrencyCollection imported;
    CurrencyCollection exported;
    CurrencyCollection feesCollected;
    CurrencyCollection feesImported;
    CurrencyCollection recovered;
    CurrencyCollection created;
    CurrencyCollection minted;

    private String getMagic() {
        return Long.toHexString(magic);
    }
}
