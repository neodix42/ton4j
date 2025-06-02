package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.masterchainInfo last:tonNode.blockIdExt state_root_hash:int256
 * init:tonNode.zeroStateIdExt = liteServer.MasterchainInfo, id 81288385
 */
@Builder
@Data
public class MasterchainInfo implements Serializable, LiteServerAnswer {
  BlockIdExt last;
  byte[] state_root_hash;
  ZeroStateIdExt init;

  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "liteServer.masterchainInfo last:tonNode.blockIdExt state_root_hash:int256 init:tonNode.zeroStateIdExt = liteServer.MasterchainInfo");

  public byte[] serialize() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 32 + ZeroStateIdExt.getSize())
        .put(last.serialize())
        .put(state_root_hash)
        .put(init.serialize())
        .array();
  }

  public static MasterchainInfo deserialize(ByteBuffer byteBuffer) {
    return MasterchainInfo.builder()
        .last(BlockIdExt.deserialize(byteBuffer))
        .state_root_hash(Utils.read(byteBuffer, 32))
        .init(ZeroStateIdExt.deserialize(byteBuffer))
        .build();
  }

  public static MasterchainInfo deserialize(byte[] byteBuffer) {
    ByteBuffer bf = ByteBuffer.wrap(byteBuffer);
    return deserialize(bf);
  }
}
