package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.VmStack;
import org.ton.ton4j.tlb.VmStackValueCell;
import org.ton.ton4j.tlb.VmStackValueInt;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * liteServer.runMethodResult mode:# id:tonNode.blockIdExt shardblk:tonNode.blockIdExt
 * shard_proof:mode.0?bytes
 * proof:mode.0?bytes
 * state_proof:mode.1?bytes
 * init_c7:mode.3?bytes
 * lib_extras:mode.4?bytes
 * exit_code:int
 * result:mode.2?bytes = liteServer.RunMethodResult;
 * </pre>
 */
@Data
@Builder
public class RunMethodResult implements Serializable, LiteServerAnswer {
  public static final int RUN_METHOD_RESULT_ANSWER = -1550163605;

  private int mode;
  private BlockIdExt id;
  private BlockIdExt shardblk;
  public byte[] shardProof;
  public byte[] proof;
  public byte[] stateProof;
  public byte[] initC7;
  public byte[] libExtras;
  private int exitCode;
  public byte[] result;

  public static final int constructorId = RUN_METHOD_RESULT_ANSWER;

  public String getProof() {
    if (proof == null) {
      return "";
    }
    return Utils.bytesToHex(proof);
  }

  public String getStateProof() {
    if (stateProof == null) {
      return "";
    }
    return Utils.bytesToHex(stateProof);
  }

  public String getInit7() {
    if (initC7 == null) {
      return "";
    }
    return Utils.bytesToHex(initC7);
  }

  public String getLibExtras() {
    if (libExtras == null) {
      return "";
    }
    return Utils.bytesToHex(libExtras);
  }

  public String getResult() {
    if (result == null) {
      return "";
    }
    return Utils.bytesToHex(result);
  }

  public static RunMethodResult deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    int mode = buffer.getInt();
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    BlockIdExt shardblk = BlockIdExt.deserialize(buffer);

    byte[] shardProof = null;
    if ((mode & 1) != 0) {
      shardProof = Utils.fromBytes(buffer);
    }

    byte[] proof = null;
    if ((mode & 1) != 0) {
      proof = Utils.fromBytes(buffer);
    }

    byte[] stateProof = null;
    if ((mode & 2) != 0) {
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

    byte[] result = null;

    if ((mode & 4) != 0) {
      result = Utils.fromBytes(buffer);
    }

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

  public BigInteger getIntFromResult(int stackIndex) {
    VmStack vmStack = VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(result)));
    VmStackValueInt result =
        VmStackValueInt.deserialize(
            CellSlice.beginParse(vmStack.getStack().getTos().get(stackIndex).toCell()));
    return result.getValue();
  }

  public Cell getCellFromResult(int stackIndex) {
    VmStack vmStack = VmStack.deserialize(CellSlice.beginParse(Cell.fromBoc(result)));
    VmStackValueCell result =
        VmStackValueCell.deserialize(
            CellSlice.beginParse(vmStack.getStack().getTos().get(stackIndex).toCell()));
    return result.getCell();
  }
}
