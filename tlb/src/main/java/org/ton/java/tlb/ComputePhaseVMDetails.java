package org.ton.java.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 *
 *
 * <pre>
 *   gas_used:(VarUInteger 7)
 *   gas_limit:(VarUInteger 7)
 *   gas_credit:(Maybe (VarUInteger 3))
 *   mode:int8
 *   exit_code:int32
 *   exit_arg:(Maybe int32)
 *   vm_steps:uint32
 *   vm_init_state_hash:bits256
 *   vm_final_state_hash:bits256
 *   </pre>
 */
@Builder
@Data
public class ComputePhaseVMDetails {
  BigInteger gasUsed;
  BigInteger gasLimit;
  BigInteger gasCredit;
  int mode;
  long exitCode;
  long exitArg;
  long vMSteps;
  BigInteger vMInitStateHash;
  BigInteger vMFinalStateHash;

  private String getVmInitStateHash() {
    return vMInitStateHash.toString(16);
  }

  private String getVmFinalStateHash() {
    return vMFinalStateHash.toString(16);
  }

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeVarUint(gasCredit, 3) // (VarUInteger 7)
        .storeVarUint(gasLimit, 3)
        .storeVarUintMaybe(gasCredit, 2)
        .storeInt(mode, 8)
        .storeInt(exitCode, 32)
        .storeIntMaybe(exitArg, 32)
        .storeUint(vMSteps, 32)
        .storeUint(vMInitStateHash, 256)
        .storeUint(vMFinalStateHash, 256)
        .endCell();
  }

  public static ComputePhaseVMDetails deserialize(CellSlice cs) {
    return ComputePhaseVMDetails.builder()
        .gasUsed(cs.loadVarUInteger(BigInteger.valueOf(3))) // (VarUInteger 7)
        .gasLimit(cs.loadVarUInteger(BigInteger.valueOf(3)))
        .gasCredit(cs.loadBit() ? cs.loadVarUInteger(BigInteger.valueOf(2)) : BigInteger.ZERO)
        .mode(cs.loadUint(8).intValue())
        .exitCode(cs.loadUint(32).longValue())
        .exitArg(cs.loadBit() ? cs.loadUint(32).longValue() : 0L)
        .vMSteps(cs.loadUint(32).longValue())
        .vMInitStateHash(cs.loadUint(256))
        .vMFinalStateHash(cs.loadUint(256))
        .build();
  }
}
