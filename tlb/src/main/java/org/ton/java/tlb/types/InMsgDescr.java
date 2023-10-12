package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * import_fees$_ fees_collected:Grams
 *   value_imported:CurrencyCollection = ImportFees;
 * _ (HashmapAugE 256 InMsg ImportFees) = InMsgDescr;
 */

public class InMsgDescr {
    TonHashMapE inMsg;
    BigInteger feesCollected;
    CurrencyCollection valueImported;
}
