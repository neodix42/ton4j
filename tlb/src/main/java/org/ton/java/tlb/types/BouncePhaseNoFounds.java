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
public class BouncePhaseNoFounds {
    int magic; // `tlb:"$01"`
    StorageUsedShort msgSize; // `tlb:"."`
    BigInteger reqFwdFees; //`tlb:"."`

    private String getMagic() {
        return Integer.toHexString(magic);
    }
}
