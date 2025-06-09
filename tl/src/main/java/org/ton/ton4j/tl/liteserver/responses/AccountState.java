package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

@Data
@Builder
public class AccountState implements Serializable, LiteServerAnswer {
  public static final int ACCOUNT_STATE_ANSWER = 1887029073; // 2ee6b589

  private BlockIdExt id;
  private BlockIdExt shardblk;
  public byte[] shardProof;
  public byte[] proof;
  public byte[] state;

  public String getShardProof() {
    return Utils.bytesToHex(shardProof);
  }

  public String getProof() {
    return Utils.bytesToHex(proof);
  }

  public String getState() {
    return Utils.bytesToHex(state);
  }

  public org.ton.ton4j.tlb.AccountState getAccountState() {
    if (state == null || state.length == 0) {
      return null;
    }
    return org.ton.ton4j.tlb.AccountState.deserialize(CellSlice.beginParse(Cell.fromBoc(state)));
  }

  public static final int constructorId = ACCOUNT_STATE_ANSWER;

  public static AccountState deserialize(ByteBuffer buffer) {

    return AccountState.builder()
        .id(BlockIdExt.deserialize(buffer))
        .shardblk(BlockIdExt.deserialize(buffer))
        .shardProof(Utils.fromBytes(buffer))
        .proof(Utils.fromBytes(buffer))
        .state(Utils.fromBytes(buffer))
        .build();
  }

  public static AccountState deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
