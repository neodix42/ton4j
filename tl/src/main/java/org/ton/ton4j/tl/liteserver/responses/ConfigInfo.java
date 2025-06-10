package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.ConfigParams;
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
public class ConfigInfo implements Serializable, LiteServerAnswer {

  public static final int CONFIG_ALL_ANSWER = -1367660753;

  public static final int constructorId = CONFIG_ALL_ANSWER;

  int mode;
  BlockIdExt id;
  public byte[] stateProof;
  public byte[] configProof;

  private String getConfigProof() {
    if (configProof != null) {
      return Utils.bytesToHex(configProof);
    }
    return "";
  }

  private String getStateProof() {
    if (stateProof != null) {
      return Utils.bytesToHex(stateProof);
    }
    return "";
  }

  public ConfigParams getConfigParsed() {
    if ((configProof == null) || (configProof.length < 10)) {
      return null;
    } else {
      return ConfigParams.deserialize(CellSlice.beginParse(Cell.fromBoc(configProof)));
    }
  }

  public byte[] serialize() {
    byte[] t1 = Utils.toBytes(stateProof);
    byte[] t2 = Utils.toBytes(configProof);
    ByteBuffer byteBuffer =
        ByteBuffer.allocate(4 + BlockIdExt.getSize() + t1.length + t2.length)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(mode)
            .put(id.serialize())
            .put(t1)
            .put(t2);
    return byteBuffer.array();
  }

  public static ConfigInfo deserialize(ByteBuffer byteBuffer) {
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    return ConfigInfo.builder()
        .mode(byteBuffer.getInt())
        .id(BlockIdExt.deserialize(byteBuffer))
        .stateProof(Utils.fromBytes(byteBuffer))
        .configProof(Utils.fromBytes(byteBuffer))
        .build();
  }

  public static ConfigInfo deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }

  public int getSize() {
    return 4
        + BlockIdExt.getSize()
        + Utils.toBytes(stateProof).length
        + Utils.toBytes(configProof).length;
  }
}
