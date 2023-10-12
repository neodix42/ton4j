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
public class TransactionShortInfo {
    BigInteger lt;
    BigInteger hash;
    BigInteger accountId;
}
