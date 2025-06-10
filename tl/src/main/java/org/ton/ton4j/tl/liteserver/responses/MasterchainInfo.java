package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
  public static final int MASTERCHAIN_INFO_ANSWER = -2055001983;
  BlockIdExt last;
  public byte[] stateRootHash;
  ZeroStateIdExt init;

  public String getStateRootHash() {
    if (stateRootHash == null) {
      return "";
    }
    return Utils.bytesToHex(stateRootHash);
  }

  // 0x81288385
  public static final int constructorId = MASTERCHAIN_INFO_ANSWER;

  public byte[] serialize() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 32 + ZeroStateIdExt.getSize())
        .order(ByteOrder.LITTLE_ENDIAN)
        .put(last.serialize())
        .put(stateRootHash)
        .put(init.serialize())
        .array();
  }

  public static MasterchainInfo deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return MasterchainInfo.builder()
        .last(BlockIdExt.deserialize(byteBuffer))
        .stateRootHash(Utils.read(byteBuffer, 32))
        .init(ZeroStateIdExt.deserialize(byteBuffer))
        .build();
  }

  public static MasterchainInfo deserialize(byte[] byteBuffer) {
    return deserialize(ByteBuffer.wrap(byteBuffer));
  }
}
