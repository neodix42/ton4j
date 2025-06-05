package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Getter
public class OneTransactionQuery implements LiteServerQueryData {
  public static final int ONE_TRANSACTION_QUERY = -737205014;

  private BlockIdExt id;
  private Address account;
  private long lt;

  public String getQueryName() {
    return "liteServer.getOneTransaction id:tonNode.blockIdExt account:liteServer.accountId lt:long = liteServer.TransactionInfo";
  }

  public byte[] getQueryData() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 4 + 32 + 8)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(ONE_TRANSACTION_QUERY)
        .put(id.serialize())
        .putInt(account.wc)
        .put(account.hashPart)
        .putLong(lt)
        .array();
  }
}
