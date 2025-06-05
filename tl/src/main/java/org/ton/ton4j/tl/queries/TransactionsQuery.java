package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class TransactionsQuery implements LiteServerQueryData {
  public static final int TRANSACTIONS_QUERY = 00;

  private int count;
  private Address account;
  private long lt;
  private byte[] hash; // 32 bytes for int256

  public String getQueryName() {
    return "liteServer.getTransactions count:# account:liteServer.accountId lt:long hash:int256 = liteServer.TransactionList";
  }

  public byte[] getQueryData() {
    if (hash.length != 32) {
      throw new IllegalArgumentException("Hash must be 32 bytes");
    }

    ByteBuffer buffer = ByteBuffer.allocate(4 + 4 + 4 + 32 + 8 + 32);
    buffer.putInt(count);
    buffer.putInt(account.wc);
    buffer.put(account.hashPart);
    buffer.putLong(lt);
    buffer.put(hash);
    return buffer.array();
  }
}
