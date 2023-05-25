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
public class GlobalVersion {
    long magic; // `tlb:"#c4"`
    long version; // `tlb:"## 32"`
    BigInteger capabilities; // `tlb:"## 64"`
}
