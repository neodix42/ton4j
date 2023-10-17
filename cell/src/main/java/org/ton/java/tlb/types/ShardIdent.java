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
    long prefixBits; //`shard_pfx_bits:(#<= 60), loading 6 bits because 60 has max 6 bits
    long workchain; //`tlb:"## 32"`
    BigInteger shardPrefix; //uint64 `tlb:"## 64"`

    private String getMagic() {
        return Long.toBinaryString(magic);
    }
}