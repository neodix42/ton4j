package org.ton.ton4j.tl.types;

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
    return org.ton.ton4j.tlb.AccountStateActive.deserialize(
        CellSlice.beginParse(Cell.fromBoc(state)));
  }

  public static final int constructorId = ACCOUNT_STATE_ANSWER;

  public static AccountState deserialize(ByteBuffer buffer) {
    BlockIdExt id = BlockIdExt.deserialize(buffer);
    BlockIdExt shardblk = BlockIdExt.deserialize(buffer);

    byte[] shardProof = Utils.fromBytes(buffer);
    byte[] proof = Utils.fromBytes(buffer);
    byte[] state = Utils.fromBytes(buffer);

    return AccountState.builder()
        .id(id)
        .shardblk(shardblk)
        .shardProof(shardProof)
        .proof(proof)
        .state(state)
        .build();
  }

  public static AccountState deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
