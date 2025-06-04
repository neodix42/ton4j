package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * ton_api.tl
 * liteServer.configInfo
 * mode:#
 * id:tonNode.blockIdExt
 * state_proof:bytes
 * config_proof:bytes = liteServer.ConfigInfo;
 * </pre>
 */
@Builder
@Data
public class ConfigAll implements Serializable, LiteServerAnswer {

  public static final int CONFIG_ALL_ANSWER = -1367660753; // 2ee6b589

  public static final int constructorId = CONFIG_ALL_ANSWER;
  //      (int)
  //          Utils.getQueryCrc32IEEEE(
  //              "liteServer.configInfo mode:# id:tonNode.blockIdExt state_proof:bytes
  // config_proof:bytes = liteServer.ConfigInfo");

  int mode;
  BlockIdExt id;
  public byte[] stateProof;
  public byte[] configProof;

  private String getConfigProof() {
    return Utils.bytesToHex(configProof);
  }

  private String getStateProof() {
    return Utils.bytesToHex(stateProof);
  }

  //  public byte[] serialize() {
  //    ByteBuffer byteBuffer =
  //        ByteBuffer.allocate(4 + 8 + 4 + 32 + 32)
  //            .order(ByteOrder.LITTLE_ENDIAN)
  //            .putInt(workchain)
  //            .putLong(shard)
  //            .putInt((int) seqno)
  //            .put(rootHash)
  //            .put(fileHash);
  //    return byteBuffer.array();
  //  }

  public static ConfigAll deserialize(ByteBuffer bf) {
    bf.order(ByteOrder.LITTLE_ENDIAN);
    return ConfigAll.builder()
        .mode(bf.getInt())
        .id(BlockIdExt.deserialize(bf))
        // .configProof(Utils.fromBytes(bs.))
        // .stateProof(null) // todo
        .build();
  }

  public static ConfigAll deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }

  public static int getSize() {
    return 4 + 8 + 4 + 32 + 32;
  }
}
