package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.ton.java.cell.TonHashMap;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class CurrencyCollection {
    BigInteger coins;
    TonHashMap extraCurrencies;
}
