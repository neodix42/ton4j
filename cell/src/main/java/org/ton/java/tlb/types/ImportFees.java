package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * import_fees$_
 * fees_collected:Grams
 * value_imported:CurrencyCollection = ImportFees;
 */

public class ImportFees {
    BigInteger feesCollected;
    CurrencyCollection valueImported;
}
