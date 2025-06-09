package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Getter;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Getter
public class AccountStateQuery implements LiteServerQueryData {
  public static final int ACCOUNT_STATE_QUERY = 1804144165;

  private BlockIdExt id;
  private Address account;

  public String getQueryName() {
    return "liteServer.getAccountState id:tonNode.blockIdExt account:liteServer.accountId = liteServer.AccountState";
  }

  public byte[] getQueryData() {
    int len = serialize().length + 4;
    ByteBuffer buffer = ByteBuffer.allocate(len);
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(ACCOUNT_STATE_QUERY);
    buffer.put(serialize());
    return buffer.array();
  }

  public byte[] serialize() {
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 32);
    buffer.put(id.serialize());
    buffer.putInt(account.wc);
    buffer.put(account.hashPart);
    return buffer.array();
  }
}
