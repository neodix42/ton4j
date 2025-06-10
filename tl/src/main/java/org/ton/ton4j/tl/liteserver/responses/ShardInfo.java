package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.shardInfo id:tonNode.blockIdExt shardblk:tonNode.blockIdExt shard_proof:bytes
 * shard_descr:bytes = liteServer.ShardInfo;
 */
@Builder
@Data
public class ShardInfo implements Serializable, LiteServerAnswer {
  public static final int SHARD_INFO_ANSWER = -1612264060;
  BlockIdExt id;
  BlockIdExt shardblk;
  public byte[] shardProof;
  public byte[] shardDescr;

  public String getShardProof() {
    if (shardProof != null) {
      return Utils.bytesToHex(shardProof);
    }
    return "";
  }

  public String getShardDescr() {
    if (shardDescr != null) {
      return Utils.bytesToHex(shardDescr);
    }
    return "";
  }

  public static final int constructorId = SHARD_INFO_ANSWER;

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(shardProof);
    byte[] t2 = Utils.toBytes(shardDescr);
    ByteBuffer buffer =
        ByteBuffer.allocate(BlockIdExt.getSize() * 2 + 4 + t1.length + 4 + t2.length);
    buffer.put(id.serialize());
    buffer.put(shardblk.serialize());
    buffer.put(t1);
    buffer.put(t2);
    return buffer.array();
  }

  public static ShardInfo deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return ShardInfo.builder()
        .id(BlockIdExt.deserialize(byteBuffer))
        .shardblk(BlockIdExt.deserialize(byteBuffer))
        .shardProof(Utils.fromBytes(byteBuffer))
        .shardDescr(Utils.fromBytes(byteBuffer))
        .build();
  }

  public static ShardInfo deserialize(byte[] byteArray) {
    return deserialize(ByteBuffer.wrap(byteArray));
  }
}
