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
public class BlockHandle {
    BigInteger offset; // The offset of the block in the file. uint64_t
    BigInteger size; // The size of the stored block. uint64_t
}
