package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.ShardHashes;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.allShardsInfo id:tonNode.blockIdExt proof:bytes data:bytes = liteServer.AllShardsInfo;
 */
@Builder
@Data
public class AllShardsInfo implements Serializable, LiteServerAnswer {
  public static final int ALL_SHARDS_INFO_ANSWER = 160425773;

  BlockIdExt id;
  public byte[] proof;
  public byte[] data;

  public String getProof() {
    if (proof != null) {
      return Utils.bytesToHex(proof);
    }
    return "";
  }

  public String getData() {
    if (data != null) {
      return Utils.bytesToHex(data);
    }
    return "";
  }

  public ShardHashes getShardHashes() {
    if (data == null || data.length == 0) {
      return null;
    }
    return org.ton.ton4j.tlb.ShardHashes.deserialize(CellSlice.beginParse(Cell.fromBoc(data)));
  }

  public static final int constructorId = ALL_SHARDS_INFO_ANSWER;

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(proof);
    byte[] t2 = Utils.toBytes(data);
    ByteBuffer buffer = ByteBuffer.allocate(BlockIdExt.getSize() + 4 + t1.length + 4 + t2.length);
    buffer.put(id.serialize());
    buffer.put(t1);
    buffer.put(t2);
    return buffer.array();
  }

  public static AllShardsInfo deserialize(ByteBuffer byteBuffer) {
    BlockIdExt id = BlockIdExt.deserialize(byteBuffer);
    byte[] proof = Utils.fromBytes(byteBuffer);
    byte[] data = Utils.fromBytes(byteBuffer);
    return AllShardsInfo.builder().id(id).proof(proof).data(data).build();
  }

  public static AllShardsInfo deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
