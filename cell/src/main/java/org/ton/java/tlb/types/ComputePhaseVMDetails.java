package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 *   gas_used:(VarUInteger 7)
 *   gas_limit:(VarUInteger 7)
 *   gas_credit:(Maybe (VarUInteger 3))
 *   mode:int8
 *   exit_code:int32
 *   exit_arg:(Maybe int32)
 *   vm_steps:uint32
 *   vm_init_state_hash:bits256
 *   vm_final_state_hash:bits256
 */
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
                .storeVarUint(gasCredit, 7)
                .storeVarUint(gasLimit, 7)
                .storeVarUintMaybe(gasCredit, 3)
                .storeInt(mode, 8)
                .storeInt(exitCode, 32)
                .storeIntMaybe(exitArg, 32)
                .storeUint(vMSteps, 32)
                .storeUint(vMInitStateHash, 256)
                .storeUint(vMFinalStateHash, 256)
                .endCell();
    }
}
