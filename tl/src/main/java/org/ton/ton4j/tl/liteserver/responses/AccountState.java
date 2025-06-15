package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.Account;
import org.ton.ton4j.tlb.ShardAccount;
import org.ton.ton4j.tlb.ShardStateUnsplit;
import org.ton.ton4j.utils.Utils;

/**
 * liteServer.accountState id:tonNode.blockIdExt shardblk:tonNode.blockIdExt shard_proof:bytes
 * proof:bytes state:bytes = liteServer.AccountState;
 */
@Data
@Builder
public class AccountState implements Serializable, LiteServerAnswer {
  public static final int ACCOUNT_STATE_ANSWER = 1887029073; // 2ee6b589

  private BlockIdExt id;
  private BlockIdExt shardblk;
  public byte[] shardProof;
  public byte[] proof;
  public byte[] state;

  public String getState() {
    if (state == null) {
      return "";
    }
    return Utils.bytesToHex(state);
  }

  public String getShardProof() {
    if (shardProof == null) {
      return "";
    }
    return Utils.bytesToHex(shardProof);
  }

  public String getProof() {
    if (proof == null) {
      return "";
    }
    return Utils.bytesToHex(proof);
  }

  public Account getAccount() {
    if (state == null || state.length == 0) {
      return null;
    }
    return Account.deserialize(CellSlice.beginParse(Cell.fromBoc(state)));
  }

  public ShardStateUnsplit getShardStateUnsplit() {
    if (proof == null || proof.length == 0) {
      return null;
    }

    List<Cell> cells = CellBuilder.beginCell().fromBocMultiRoot(proof).endCells();

    return ShardStateUnsplit.deserialize(CellSlice.beginParse(cells.get(1).getRefs().get(0)));
  }

  public List<ShardAccount> getShardAccounts() {
    if (proof == null || proof.length == 0) {
      return null;
    }

    List<Cell> cells = CellBuilder.beginCell().fromBocMultiRoot(proof).endCells();

    //    return null;
    return ShardStateUnsplit.deserialize(CellSlice.beginParse(cells.get(1).getRefs().get(0)))
        .getShardAccounts()
        .getShardAccountsAsList();
  }

  public static final int constructorId = ACCOUNT_STATE_ANSWER;

  public static AccountState deserialize(ByteBuffer buffer) {
    buffer.order(ByteOrder.LITTLE_ENDIAN);
    AccountState accountState =
        AccountState.builder()
            .id(BlockIdExt.deserialize(buffer))
            .shardblk(BlockIdExt.deserialize(buffer))
            .build();

    ByteBuffer slice = buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN);

    if (slice.getInt() != 0) {
      accountState.setShardProof(Utils.fromBytes(buffer));
    } else {
      buffer.position(buffer.position() + 4);
    }
    accountState.setProof(Utils.fromBytes(buffer));
    accountState.setState(Utils.fromBytes(buffer));

    return accountState;
  }

  public static AccountState deserialize(byte[] data) {
    return deserialize(ByteBuffer.wrap(data));
  }
}
