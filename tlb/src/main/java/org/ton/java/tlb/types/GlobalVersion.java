package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class GlobalVersion {
    long magic; // `tlb:"#c4"`
    long version; // `tlb:"## 32"`
    BigInteger capabilities; // `tlb:"## 64"`
}
