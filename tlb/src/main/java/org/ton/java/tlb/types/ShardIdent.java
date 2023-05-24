package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class ShardIdent {
    long magic; // `tlb:"$00"`
    byte prefixBits; //`tlb:"## 6"` // #<= 60
    int workchain; //`tlb:"## 32"`
    BigInteger shardPrefix; //uint64 `tlb:"## 64"`
}
