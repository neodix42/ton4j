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
public class ComputePhaseVMDetails {
    BigInteger gasUsed; //         *big.Int `tlb:"var uint 7"`
    BigInteger gasLimit; //        *big.Int `tlb:"var uint 7"`
    BigInteger gasCredit; //        *big.Int `tlb:"maybe var uint 3"`
    int mode; //; //             int8     `tlb:"## 8"`
    long exitCode; //         int32    `tlb:"## 32"`
    long exitArg; //          *int32   `tlb:"maybe ## 32"`
    long vMSteps; //          uint32   `tlb:"## 32"`
    BigInteger vMInitStateHash; //  []byte   `tlb:"bits 256"`
    BigInteger vMFinalStateHash; // []byte   `tlb:"bits 256"`

    private String getVMInitStateHash() {
        return vMInitStateHash.toString(16);
    }

    private String getVMFinalStateHash() {
        return vMFinalStateHash.toString(16);
    }
}
