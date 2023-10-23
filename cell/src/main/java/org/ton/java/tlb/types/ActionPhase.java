package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * tr_phase_action$_ success:Bool valid:Bool no_funds:Bool
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
}
