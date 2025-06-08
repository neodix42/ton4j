package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * liteServer.shardBlockProof masterchain_id:tonNode.blockIdExt links:(vector
 * liteServer.shardBlockLink) = liteServer.ShardBlockProof;
 */
@Builder
@Data
public class ShardBlockProof implements Serializable, LiteServerAnswer {

  private final BlockIdExt masterchainId;
  private final List<ShardBlockLink> links;

  public static final int constructorId = 493002874;

  public static ShardBlockProof deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    BlockIdExt masterchainId = BlockIdExt.deserialize(buffer);
    int vectorLength = buffer.getInt();
    List<ShardBlockLink> links = new ArrayList<>(vectorLength);

    for (int i = 0; i < vectorLength; i++) {
      links.add(ShardBlockLink.deserialize(buffer));
    }

    return ShardBlockProof.builder().masterchainId(masterchainId).links(links).build();
  }

  public static ShardBlockProof deserialize(byte[] byteBuffer) {
    return deserialize(ByteBuffer.wrap(byteBuffer));
  }

  //
  //    public byte[] serialize() {
  //        int size = BlockIdExt.getSize() + 4;
  //        for (ShardBlockLink link : links) {
  //            size += link.serialize().length;
  //        }
  //
  //        ByteBuffer buffer = ByteBuffer.allocate(size)
  //                .order(ByteOrder.LITTLE_ENDIAN)
  //                .put(masterchainId.serialize())
  //                .putInt(links.size());
  //
  //        for (ShardBlockLink link : links) {
  //            buffer.put(link.serialize());
  //        }
  //
  //        return buffer.array();
  //    }
}
