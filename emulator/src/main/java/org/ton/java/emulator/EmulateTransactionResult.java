package org.ton.java.emulator;

import java.io.Serializable;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.tlb.types.OutList;
import org.ton.java.tlb.types.ShardAccount;
import org.ton.java.tlb.types.Transaction;

@Builder
@Data
public class EmulateTransactionResult implements Serializable {
  boolean success;
  String error;
  boolean external_not_accepted;
  String transaction; // "Base64 encoded Transaction boc"
  String shard_account; // "Base64 encoded new ShardAccount boc"
  int vm_exit_code;
  String vm_log; //  "execute DUP..."
  String actions; // Base64 encoded compute phase actions boc (OutList n)"
  double elapsed_time;

  ShardAccount getNewShardAccount() {
    if (StringUtils.isNotEmpty(transaction)) {
      return ShardAccount.deserialize(CellSlice.beginParse(Cell.fromBocBase64(shard_account)));
    } else {
      return ShardAccount.builder().build();
    }
  }

  Transaction getTransaction() {
    if (StringUtils.isNotEmpty(transaction)) {
      return Transaction.deserialize(CellSlice.beginParse(Cell.fromBocBase64(transaction)));
    } else {
      return Transaction.builder().build();
    }
  }

  OutList getActions() {
    if (StringUtils.isNotEmpty(transaction)) {
      return OutList.deserialize(CellSlice.beginParse(Cell.fromBocBase64(actions)));
    } else {
      return OutList.builder().build();
    }
  }
}
