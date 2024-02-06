package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class ValidatorAddr {
    long magic;             //Magic            `tlb:"#73"`
    SigPubKeyED25519 publicKey;//SigPubKeyED25519 `tlb:"."`
    BigInteger weight;      //uint64           `tlb:"## 64"`
    BigInteger adnlAddr;        //[]byte           `tlb:"bits 256"`

    private String getMagic() {
        return Long.toHexString(magic);
    }

    private String getAdnlAddr() {
        return adnlAddr.toString(16);
    }

    public static ValidatorAddr deserialize(CellSlice cs) {
        return null;
    }
}
