package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.masterchainInfoExt mode:# version:int capabilities:long last:tonNode.blockIdExt
 * last_utime:int now:int state_root_hash:int256 init:tonNode.zeroStateIdExt =
 * liteServer.MasterchainInfoExt;
 */
@Builder
@Data
public class MasterchainInfoExt implements Serializable, LiteServerAnswer {
  public static final int MASTERCHAIN_INFO_EXT_ANSWER = -1462968075;
  int mode;
  int version;
  long capabilities;
  BlockIdExt last;
  int lastTime;
  int now;
  public byte[] stateRootHash;
  ZeroStateIdExt init;

  public String getStateRootHash() {
    if (stateRootHash == null) {
      return "";
    }
    return Utils.bytesToHex(stateRootHash);
  }

  public static final int constructorId = MASTERCHAIN_INFO_EXT_ANSWER;

  public byte[] serialize() {
    return ByteBuffer.allocate(BlockIdExt.getSize() + 32 + ZeroStateIdExt.getSize())
        .order(ByteOrder.LITTLE_ENDIAN)
        .putInt(mode)
        .putInt(version)
        .putLong(capabilities)
        .put(last.serialize())
        .putInt(lastTime)
        .putInt(now)
        .put(stateRootHash)
        .put(init.serialize())
        .array();
  }

  public static MasterchainInfoExt deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return MasterchainInfoExt.builder()
        .mode(byteBuffer.getInt())
        .version(byteBuffer.getInt())
        .capabilities(byteBuffer.getLong())
        .last(BlockIdExt.deserialize(byteBuffer))
        .lastTime(byteBuffer.getInt())
        .now(byteBuffer.getInt())
        .stateRootHash(Utils.read(byteBuffer, 32))
        .init(ZeroStateIdExt.deserialize(byteBuffer))
        .build();
  }

  public static MasterchainInfoExt deserialize(byte[] byteBuffer) {
    return deserialize(ByteBuffer.wrap(byteBuffer));
  }
}
