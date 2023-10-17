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
public class StorageInfo {
    StorageUsed storageUsed;
    long lastPaid; // uint32
    BigInteger duePayment; // bigInt
}
