package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class AccountStateQuery implements LiteServerQueryData {
  public static final int ACCOUNT_STATE_QUERY = 1804144165;

  private BlockIdExt id;
  private Address account;

  public String getQueryName() {
    return "liteServer.getAccountState id:tonNode.blockIdExt account:liteServer.accountId = liteServer.AccountState";
  }

  public byte[] getQueryData() {
    int len = serialize().length + 4;
    return ByteBuffer.allocate(len)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(ACCOUNT_STATE_QUERY)
        .put(serialize())
        .array();
  }

  public byte[] serialize() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 32)
        .put(id.serialize())
        .putInt(account.wc)
        .put(account.hashPart)
        .array();
  }
}
