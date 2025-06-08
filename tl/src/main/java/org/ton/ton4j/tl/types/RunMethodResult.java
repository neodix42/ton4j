package org.ton.ton4j.tl.types;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class RunMethodResult implements Serializable, LiteServerAnswer {
  public static final int SEND_MSG_STATUS_ANSWER = 60452816;

  private int mode;
  private BlockIdExt id;
  private BlockIdExt shardblk;
  private byte[] shardProof;
  private byte[] proof;
  private byte[] stateProof;
  private byte[] initC7;
  private byte[] libExtras;
  private int exitCode;
  private byte[] result;

  public static final int constructorId = SEND_MSG_STATUS_ANSWER;

  //      (int)
  //          Utils.getQueryCrc32IEEEE(
  //              "liteServer.runMethodResult mode:# id:tonNode.blockIdExt
  // shardblk:tonNode.blockIdExt shard_proof:maybe(bytes) proof:maybe(bytes)
  // state_proof:maybe(bytes) init_c7:maybe(bytes) lib_extras:maybe(bytes) exit_code:int
  // result:bytes = liteServer.RunMethodResult");

  public static RunMethodResult deserialize(ByteBuffer buffer) {
    int mode = buffer.getInt();
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    BlockIdExt shardblk = BlockIdExt.deserialize(buffer);

    byte[] shardProof = null;
    if ((mode & 1) != 0) {
      shardProof = Utils.fromBytes(buffer);
    }

    byte[] proof = null;
    if ((mode & 2) != 0) {
      proof = Utils.fromBytes(buffer);
    }

    byte[] stateProof = null;
    if ((mode & 4) != 0) {
      stateProof = Utils.fromBytes(buffer);
    }

    byte[] initC7 = null;
    if ((mode & 8) != 0) {
      initC7 = Utils.fromBytes(buffer);
    }

    byte[] libExtras = null;
    if ((mode & 16) != 0) {
      libExtras = Utils.fromBytes(buffer);
    }

    int exitCode = buffer.getInt();

    byte[] result = Utils.fromBytes(buffer);

    return RunMethodResult.builder()
        .mode(mode)
        .id(id)
        .shardblk(shardblk)
        .shardProof(shardProof)
        .proof(proof)
        .stateProof(stateProof)
        .initC7(initC7)
        .libExtras(libExtras)
        .exitCode(exitCode)
        .result(result)
        .build();
  }

  public static RunMethodResult deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
