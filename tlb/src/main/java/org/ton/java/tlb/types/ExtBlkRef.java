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
public class ExtBlkRef {
    BigInteger endLt; // `tlb:"## 64"`
    int seqno; // `tlb:"## 32"`
    int[] rootHash; // `tlb:"bits 256"`
    int[] fileHash;
}
