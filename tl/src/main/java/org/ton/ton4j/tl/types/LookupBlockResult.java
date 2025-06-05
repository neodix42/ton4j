package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
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
  BlockId id;
  int mode;
  BlockIdExt mc_block_id;
  byte[] clientMcStateProof;
  byte[] mcBlockProof;
  List<ShardInfo> shards; // todo shardBlockLink
  byte[] header;
  byte[] prevHeader;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.lookupBlockResult id:tonNode.blockIdExt mode:# mc_block_id:tonNode.blockIdExt client_mc_state_proof:bytes mc_block_proof:bytes shard_links:(vector liteServer.shardBlockLink) header:bytes prev_header:bytes = liteServer.LookupBlockResult");

  public LookupBlockResult deserialize(ByteBuffer byteBuffer) {
    BlockId blockId = BlockId.deserialize(byteBuffer);
    int mode = byteBuffer.getInt();

    BlockIdExt mc_block_id = BlockIdExt.deserialize(byteBuffer);
    clientMcStateProof = Utils.fromBytes(byteBuffer);
    mcBlockProof = Utils.fromBytes(byteBuffer);
    shards = null;
    header = Utils.fromBytes(byteBuffer);
    prevHeader = Utils.fromBytes(byteBuffer);

    return LookupBlockResult.builder()
        .id(id)
        .mode(mode)
        .mc_block_id(mc_block_id)
        .clientMcStateProof(clientMcStateProof)
        .mcBlockProof(mcBlockProof)
        .shards(null)
        .header(header)
        .prevHeader(prevHeader)
        .build();
  }
}
