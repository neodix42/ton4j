package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
public class SigPubKeyED25519 {
    long magic;     // `tlb:"#8e81278a"`
    byte[] key;     // `tlb:"bits 256"`

    private String getMagic() {
        return Long.toHexString(magic);
    }
}
