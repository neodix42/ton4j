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
public class ComputePhaseVM {
    int magic; //  `tlb:"$1"`
    boolean success; // `tlb:"bool"`
    boolean msgStateUsed; // `tlb:"bool"`
    boolean accountActivated; // `tlb:"bool"`
    BigInteger gasFees; // `tlb:"."`

    ComputePhaseVMDetails details; // `tlb:"^"`

    private String getMagic() {
        return Integer.toHexString(magic);
    }

}
