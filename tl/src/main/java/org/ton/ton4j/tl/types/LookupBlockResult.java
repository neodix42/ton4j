package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.lookupBlockResult id:tonNode.blockIdExt mode:# mc_block_id:tonNode.blockIdExt
 * client_mc_state_proof:bytes mc_block_proof:bytes shard_links:(vector liteServer.shardBlockLink)
 * header:bytes prev_header:bytes = liteServer.LookupBlockResult;
 */
@Builder
@Data
public class LookupBlockResult implements Serializable, LiteServerAnswer {
  public static final int LOOKUP_BLOCK_ANSWER = 0;
  int mode;
  BlockId id;
  BlockIdExt mc_block_id;
  Long lt;
  Integer utime;
  byte[] proof;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.lookupBlockResult id:tonNode.blockIdExt mode:# mc_block_id:tonNode.blockIdExt client_mc_state_proof:bytes mc_block_proof:bytes shard_links:(vector liteServer.shardBlockLink) header:bytes prev_header:bytes = liteServer.LookupBlockResult");

  public byte[] serialize() {
    // Calculate size: mode (4) + BlockId size + BlockIdExt size + proof length (4+len) +
    // conditional fields
    int size = 4 + BlockId.getSize() + BlockIdExt.getSize() + 4 + proof.length;
    if ((mode & 1) != 0) size += 8;
    if ((mode & 2) != 0) size += 4;

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.putInt(mode);
    buffer.put(id.serialize());
    buffer.put(mc_block_id.serialize());

    if ((mode & 1) != 0) {
      buffer.putLong(lt);
    }
    if ((mode & 2) != 0) {
      buffer.putInt(utime);
    }

    buffer.putInt(proof.length);
    buffer.put(proof);

    return buffer.array();
  }

  public static LookupBlockResult deserialize(ByteBuffer byteBuffer) {
    int mode = byteBuffer.getInt();
    BlockId id = BlockId.deserialize(byteBuffer);
    BlockIdExt mc_block_id = BlockIdExt.deserialize(byteBuffer);

    Long lt = null;
    if ((mode & 1) != 0) {
      lt = byteBuffer.getLong();
    }

    Integer utime = null;
    if ((mode & 2) != 0) {
      utime = byteBuffer.getInt();
    }

    int proofLen = byteBuffer.getInt();
    byte[] proof = new byte[proofLen];
    byteBuffer.get(proof);

    return LookupBlockResult.builder()
        .mode(mode)
        .id(id)
        .mc_block_id(mc_block_id)
        .lt(lt)
        .utime(utime)
        .proof(proof)
        .build();
  }

  public static LookupBlockResult deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
