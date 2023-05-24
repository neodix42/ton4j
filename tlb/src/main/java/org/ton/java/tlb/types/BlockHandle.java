package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Builder
@Getter
@Setter
public class BlockHandle {
    BigInteger offset; // The offset of the block in the file. uint64_t
    BigInteger size; // The size of the stored block. uint64_t
}
