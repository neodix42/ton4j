package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * tr_phase_action$_
 *   success:Bool
 *   valid:Bool
 *   no_funds:Bool
 *   status_change:AccStatusChange
 *   total_fwd_fees:(Maybe Grams)
 *   total_action_fees:(Maybe Grams)
 *   result_code:int32
 *   result_arg:(Maybe int32)
 *   tot_actions:uint16
 *   spec_actions:uint16
 *   skipped_actions:uint16
 *   msgs_created:uint16
 *   action_list_hash:bits256
 *   tot_msg_size:StorageUsed
 *   = TrActionPhase;
 *   </pre>
 */
@Builder
@Data
public class ActionPhase implements Serializable {
  boolean success;
  boolean valid;
  boolean noFunds;
  AccStatusChange statusChange;
  BigInteger totalFwdFees;
  BigInteger totalActionFees;
  long resultCode;
  BigInteger resultArg;
  long totalActions;
  long specActions;
  long skippedActions;
  long messagesCreated;
  BigInteger actionListHash;
  StorageUsed totalMsgSize;

  private String getActionListHash() {
    return actionListHash.toString(16);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeBit(success)
        .storeBit(valid)
        .storeBit(noFunds)
        .storeCell(statusChange.toCell())
        .storeCoinsMaybe(totalFwdFees)
        .storeCoinsMaybe(totalActionFees)
        .storeInt(resultCode, 32)
        .storeIntMaybe(resultArg, 32)
        .storeUint(totalActions, 16)
        .storeUint(specActions, 16)
        .storeUint(skippedActions, 16)
        .storeUint(messagesCreated, 16)
        .storeUintMaybe(actionListHash, 256)
        .storeSlice(CellSlice.beginParse(totalMsgSize.toCell()))
        .endCell();
  }

  public static ActionPhase deserialize(CellSlice cs) {
    return ActionPhase.builder()
        .success(cs.loadBit())
        .valid(cs.loadBit())
        .noFunds(cs.loadBit())
        .statusChange(AccStatusChange.deserialize(cs))
        .totalFwdFees(cs.loadBit() ? cs.loadCoins() : null)
        .totalActionFees(cs.loadBit() ? cs.loadCoins() : null)
        .resultCode(cs.loadInt(32).longValue())
        .resultArg(cs.loadBit() ? cs.loadInt(32) : null)
        .totalActions(cs.loadUint(16).longValue())
        .specActions(cs.loadUint(16).longValue())
        .skippedActions(cs.loadUint(16).longValue())
        .messagesCreated(cs.loadUint(16).longValue())
        .actionListHash(cs.loadUint(256))
        .totalMsgSize(StorageUsed.deserialize(cs))
        .build();
  }
}
