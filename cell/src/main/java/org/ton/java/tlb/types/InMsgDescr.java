package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.TonHashMapAugE;

@Builder
@Getter
@Setter
@ToString
/**
 * _ (HashmapAugE 256 InMsg ImportFees) = InMsgDescr;
 */

public class InMsgDescr {
    TonHashMapAugE inMsg;
}
