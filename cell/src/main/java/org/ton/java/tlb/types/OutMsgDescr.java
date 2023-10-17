package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMapE;

@Builder
@Getter
@Setter
@ToString
/**
 * _ (HashmapAugE 256 OutMsg CurrencyCollection) = OutMsgDescr;
 */

public class OutMsgDescr {
    TonHashMapE outMsg;
    CurrencyCollection currencyCollection;
}
