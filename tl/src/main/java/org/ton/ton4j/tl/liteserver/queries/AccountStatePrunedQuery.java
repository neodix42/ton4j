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
public class AccountStatePrunedQuery implements LiteServerQueryData {
  public static final int ACCOUNT_STATE_PRUNED_QUERY = 1516864775;

  private BlockIdExt id;
  public Address account;

  public String getQueryName() {
    return "liteServer.getAccountStatePrunned id:tonNode.blockIdExt account:liteServer.accountId = liteServer.AccountState";
  }

  public byte[] getQueryData() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 4 + 4 + 32)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(ACCOUNT_STATE_PRUNED_QUERY)
        .put(id.serialize())
        .putInt(account.wc)
        .put(account.hashPart)
        .array();
  }
}
