package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import static java.util.Objects.isNull;

/**
 *
 *
 * <pre>
 * new_bounce_body#fffffffe
 *   original_body:^Cell
 *   original_info:^NewBounceOriginalInfo
 *   bounced_by_phase:uint8
 *   exit_code:int32
 *   compute_phase:(Maybe NewBounceComputePhaseInfo) = NewBounceBody;
 *
 * original_body - cell that contains the body of the original message.
 * If extra_flags & 2 then the whole body is returned, otherwise it is only the root without refs.
 * original_info - value, lt and unixtime of the original message.
 * bounced_by_phase:
 * 0 - compute phase was skipped. exit_code denotes the skip reason:
 *   exit_code = -1 - no state (account is uninit or frozen, and no state init is present in the message).
 *   exit_code = -2 - bad state (account is uninit or frozen, and state init in the message has the wrong hash).
 *   exit_code = -3 - no gas.
 *   exit_code = -4 - account is suspended.
 * 1 - compute phase failed. exit_code is the value from the compute phase.
 * 2 - action phase failed. exit_code is the value from the action phase.
 * exit_code - 32-bit exit code, see above.
 * compute_phase - exists if it was not skipped (bounced_by_phase > 0):
 * gas_used, vm_steps - same as in TrComputePhase of the transaction.
 * The bounced message has the same 0th and 1st bits in extra_flags as the original message.
 * </pre>
 */
@Builder
@Data
public class NewBounceBody implements Serializable {
  long magic;
  Cell originalBody;
  NewBounceOriginalInfo originalInfo;
  int bouncedByPhase;
  long exitCode;
  NewBounceComputePhaseInfo computePhase;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xfffffffe, 32)
        .storeRef(originalBody)
        .storeRef(originalInfo.toCell())
        .storeUint(bouncedByPhase, 8)
        .storeUint(exitCode, 32)
        .storeRefMaybe(isNull(computePhase) ? null : computePhase.toCell())
        .endCell();
  }

  public static NewBounceBody deserialize(CellSlice cs) {
    NewBounceBody newBounceBody =
        NewBounceBody.builder()
            .magic(cs.loadUint(32).longValue())
            .originalBody(cs.loadRef())
            .originalInfo(NewBounceOriginalInfo.deserialize(CellSlice.beginParse(cs.loadRef())))
            .bouncedByPhase(cs.loadUint(8).intValue())
            .exitCode(cs.loadUint(32).longValue())
            .build();
    if (cs.loadBit()) {
      newBounceBody.setComputePhase(
          NewBounceComputePhaseInfo.deserialize(CellSlice.beginParse(cs.loadRef())));
    }
    return newBounceBody;
  }
}
