package org.ton.java.smartcontract.types;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.ton.java.cell.Cell;

import java.math.BigInteger;
import java.util.List;

/** MultiSig order containing n, 1&lt;= n &lt;=3, internal messages */
@Builder
@Getter
@ToString
public class Order {
  long walletId; // 32
  BigInteger queryId; // 64, default timeout 7200s, 2 hours
  List<Cell> internalMsgs;
}
