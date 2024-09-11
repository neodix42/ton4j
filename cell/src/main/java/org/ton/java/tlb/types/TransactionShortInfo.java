package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Data;

import java.math.BigInteger;

@Builder
@Data
public class TransactionShortInfo {
    BigInteger lt;
    BigInteger hash;
    BigInteger accountId;
}
