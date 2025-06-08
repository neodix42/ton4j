package org.ton.ton4j.tl.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.types.BlockIdExt;
import org.ton.ton4j.tl.types.LiteServerQueryData;

@Builder
@Data
public class ShardBlockProofQuery implements LiteServerQueryData {
  public static final int GET_SHARD_BLOCK_PROOF_QUERY = 1285948240;

  private final BlockIdExt id;

  public String getQueryName() {
    return "liteServer.getShardBlockProof id:tonNode.blockIdExt = liteServer.ShardBlockProof";
  }

  public byte[] getQueryData() {
    byte[] idBytes = id.serialize();
    return ByteBuffer.allocate(idBytes.length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(GET_SHARD_BLOCK_PROOF_QUERY)
        .put(idBytes)
        .array();
  }
}
