package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class ComputePhaseSkipped {
    int magic;     // `tlb:"$0"`
    ComputeSkipReason reason; // `tlb:"."`

    private String getMagic() {
        return Integer.toHexString(magic);
    }
}
