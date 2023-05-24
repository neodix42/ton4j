package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class ExtBlkRef {
    BigInteger endLt; // `tlb:"## 64"`
    int seqno; // `tlb:"## 32"`
    byte[] rootHash; // `tlb:"bits 256"`
    byte[] fileHash;
}
