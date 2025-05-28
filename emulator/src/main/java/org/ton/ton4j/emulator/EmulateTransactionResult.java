package org.ton.ton4j.emulator;

import java.io.Serializable;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.*;

@Builder
@Data
public class EmulateTransactionResult implements Serializable {
  boolean success;
  String error;
  boolean external_not_accepted;
  @ToString.Exclude String transaction; // "Base64 encoded Transaction boc"

  @ToString.Exclude String shard_account; // "Base64 encoded new ShardAccount boc"

  int vm_exit_code;
  String vm_log; //  "execute DUP..."
  @ToString.Exclude String actions; // Base64 encoded compute phase actions boc (OutList n)"
  double elapsed_time;

  public ShardAccount getNewShardAccount() {
    if (StringUtils.isNotEmpty(shard_account)) {
      return ShardAccount.deserialize(CellSlice.beginParse(Cell.fromBocBase64(shard_account)));
    } else {
      return ShardAccount.builder().build();
    }
  }

  public Address getNewShardAccountAddress() {
    if (StringUtils.isNotEmpty(shard_account)) {
      return ShardAccount.deserialize(CellSlice.beginParse(Cell.fromBocBase64(shard_account)))
          .getAccount()
          .getAddress()
          .toAddress();
    } else {
      return null;
    }
  }

  @ToString.Include(name = "transactionBase64")
  public String getTransactionBase64() {
    return transaction;
  }

  @ToString.Include(name = "shardAccountBase64")
  public String getShardAccountBase64() {
    return shard_account;
  }

  @ToString.Include(name = "actionsBase64")
  public String getActionsBase64() {
    return actions;
  }

  public Transaction getTransaction() {
    if (StringUtils.isNotEmpty(transaction)) {
      return Transaction.deserialize(CellSlice.beginParse(Cell.fromBocBase64(transaction)));
    } else {
      return Transaction.builder().build();
    }
  }

  public OutList getActions() {
    if (StringUtils.isNotEmpty(actions)) {
      return OutList.deserialize(CellSlice.beginParse(Cell.fromBocBase64(actions)));
    } else {
      return OutList.builder().build();
    }
  }

  public StateInit getNewStateInit() {
    ShardAccount shardAccount;
    if (StringUtils.isNotEmpty(shard_account)) {
      shardAccount =
          ShardAccount.deserialize(CellSlice.beginParse(Cell.fromBocBase64(shard_account)));
    } else {
      shardAccount = ShardAccount.builder().build();
    }

    AccountState accountState = shardAccount.getAccount().getAccountStorage().getAccountState();
    if (accountState instanceof AccountStateActive) {
      return ((AccountStateActive) accountState).getStateInit();
    }

    return StateInit.builder().build();
  }
}
