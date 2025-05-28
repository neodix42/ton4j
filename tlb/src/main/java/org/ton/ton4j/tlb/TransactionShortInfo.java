package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class TransactionShortInfo implements Serializable {
  BigInteger lt;
  BigInteger hash;
  BigInteger accountId;
}
