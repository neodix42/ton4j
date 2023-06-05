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
public class ShardIdent {
    long magic; // `tlb:"$00"`
    byte prefixBits; //`tlb:"## 6"` // #<= 60
    long workchain; //`tlb:"## 32"`
    BigInteger shardPrefix; //uint64 `tlb:"## 64"`
}