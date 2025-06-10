package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class TransactionListQuery implements LiteServerQueryData {
  public static final int TRANSACTION_LIST_QUERY = 474015649;

  private int count;
  private Address account;
  private long lt;
  public byte[] hash;

  public String getHash() {
    if (hash == null) {
      return "";
    }
    return Utils.bytesToHex(hash);
  }

  public String getQueryName() {
    return "liteServer.getTransactions count:# account:liteServer.accountId lt:long hash:int256 = liteServer.TransactionList";
  }

  public byte[] getQueryData() {
    if (hash.length != 32) {
      throw new IllegalArgumentException("Hash must be 32 bytes");
    }

    return ByteBuffer.allocate(4 + 4 + 4 + 32 + 8 + 32)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(TRANSACTION_LIST_QUERY)
        .putInt(count)
        .putInt(account.wc)
        .put(account.hashPart)
        .putLong(lt)
        .put(hash)
        .array();
  }
}
