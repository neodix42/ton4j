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
 msg_export_deq_short$1101
 msg_env_hash:bits256
 next_workchain:int32
 next_addr_pfx:uint64
 import_block_lt:uint64 = OutMsg;
 */
public class OutMsgDeqShort implements OutMsg {

    BigInteger msgEnvHash;
    long nextWorkchain;
    BigInteger nextAddrPfx;
    BigInteger importBlockLt;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0b1101, 4)
                .storeUint(msgEnvHash, 256)
                .storeInt(nextWorkchain, 32)
                .storeUint(nextAddrPfx, 64)
                .storeUint(importBlockLt, 64)
                .endCell();
    }
}
