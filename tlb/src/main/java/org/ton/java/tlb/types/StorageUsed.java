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
public class StorageUsed {
    BigInteger bitsUsed; // uint64
    BigInteger cellsUsed; // uint64
    BigInteger publicCellsUsed; // uint64
}
