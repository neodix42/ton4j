package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
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
 *   tot_msg_size:StorageUsedShort
 *   = TrActionPhase;
 */
public class ActionPhase {
    boolean success;
    boolean valid;
    boolean noFunds;
    AccStatusChange statusChange;
    BigInteger totalFwdFees;
    BigInteger totalActionFees;
    long resultCode;
    long resultArg;
    long totalActions;
    long specActions;
    long skippedActions;
    long messagesCreated;
    BigInteger actionListHash;
    StorageUsedShort totalMsgSize;

    private String getActionListHash() {
        return actionListHash.toString(16);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeBit(success)
                .storeBit(valid)
                .storeBit(noFunds)
                .storeSlice(CellSlice.beginParse(((AccStatusChange) statusChange).toCell()))
                .storeCoinsMaybe(totalFwdFees)
                .storeCoinsMaybe(totalActionFees)
                .storeInt(resultCode, 32)
                .storeIntMaybe(resultArg, 32)
                .storeUint(totalActions, 16)
                .storeUint(specActions, 16)
                .storeUint(skippedActions, 16)
                .storeUint(messagesCreated, 16)
                .storeUintMaybe(actionListHash, 256)
                .storeSlice(CellSlice.beginParse(((StorageUsedShort) totalMsgSize).toCell()))
                .endCell();
    }
}
