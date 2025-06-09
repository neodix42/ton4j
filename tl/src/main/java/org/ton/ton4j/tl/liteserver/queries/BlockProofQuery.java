package org.ton.ton4j.tl.liteserver.queries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tl.liteserver.responses.BlockIdExt;
import org.ton.ton4j.tl.liteserver.responses.LiteServerQueryData;

@Builder
@Data
public class BlockProofQuery implements LiteServerQueryData {
  public static final int BLOCK_PROOF_QUERY = -1964336060;

  private final int mode;
  private final BlockIdExt knownBlock;
  private final BlockIdExt targetBlock;

  public String getQueryName() {
    return "liteServer.getBlockProof mode:# known_block:tonNode.blockIdExt target_block:mode.0?tonNode.blockIdExt = liteServer.PartialBlockProof";
  }

  public byte[] getQueryData() {
    int size = 4 + BlockIdExt.getSize();
    if ((mode & 1) != 0) size += BlockIdExt.getSize();

    ByteBuffer buffer =
        ByteBuffer.allocate(size)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(mode)
            .put(knownBlock.serialize());

    if ((mode & 1) != 0) {
      buffer.put(targetBlock.serialize());
    }

    return ByteBuffer.allocate(buffer.array().length + 4)
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(BLOCK_PROOF_QUERY)
        .put(buffer.array())
        .array();
  }
}
