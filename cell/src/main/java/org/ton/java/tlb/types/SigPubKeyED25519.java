package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.CellSlice;

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

    public static SigPubKeyED25519 deserialize(CellSlice cs) {
        return null;
    }
}
