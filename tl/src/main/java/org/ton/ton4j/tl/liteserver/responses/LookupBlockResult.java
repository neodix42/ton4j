package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
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
  public static final int LOOKUP_BLOCK_ANSWER = -1720161305;
  BlockIdExt id;
  int mode;
  BlockIdExt mc_block_id;
  public byte[] clientMcStateProof;
  public byte[] mcBlockProof;
  List<ShardBlockLink> shardBlockLinks;
  public byte[] header;
  public byte[] prevHeader;

  String getClientMcStateProof() {
    return Utils.bytesToHex(clientMcStateProof);
  }

  String getMcBlockProof() {
    return Utils.bytesToHex(mcBlockProof);
  }

  String getHeader() {
    return Utils.bytesToHex(header);
  }

  String getPrevHeader() {
    return Utils.bytesToHex(prevHeader);
  }

  //  public BlockHeader getHeaderParsed() {
  //    if ((header == null) || (header.length < 10)) {
  //      return null;
  //    } else {
  //      return BlockHeader.deserialize(CellSlice.beginParse(Cell.fromBoc(header)));
  //    }
  //  }

  public static final int constructorId = LOOKUP_BLOCK_ANSWER;

  public static LookupBlockResult deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt blockId = BlockIdExt.deserialize(byteBuffer);
    int mode = byteBuffer.getInt();

    BlockIdExt mc_block_id = BlockIdExt.deserialize(byteBuffer);
    byte[] clientMcStateProof = Utils.fromBytes(byteBuffer);
    byte[] mcBlockProof = Utils.fromBytes(byteBuffer);
    int shardCount = byteBuffer.getInt();
    List<ShardBlockLink> shardBlockLinks = new ArrayList<>(shardCount);
    for (int i = 0; i < shardCount; i++) {
      shardBlockLinks.add(ShardBlockLink.deserialize(byteBuffer));
    }
    byte[] header = Utils.fromBytes(byteBuffer);
    byte[] prevHeader = Utils.fromBytes(byteBuffer);

    return LookupBlockResult.builder()
        .id(blockId)
        .mode(mode)
        .mc_block_id(mc_block_id)
        .clientMcStateProof(clientMcStateProof)
        .mcBlockProof(mcBlockProof)
        .shardBlockLinks(shardBlockLinks)
        .header(header)
        .prevHeader(prevHeader)
        .build();
  }

  public static LookupBlockResult deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
