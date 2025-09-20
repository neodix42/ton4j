package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_export_deq_short$1101
 * msg_env_hash:bits256
 * next_workchain:int32
 * next_addr_pfx:uint64
 * import_block_lt:uint64 = OutMsg;
 * </pre>
 */
@Builder
@Data
@Slf4j
public class OutMsgDeqShort implements OutMsg, Serializable {
  int magic;
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

  public static OutMsgDeqShort deserialize(CellSlice cs) {

    return OutMsgDeqShort.builder()
        .magic(cs.loadUint(4).intValue())
        .msgEnvHash(cs.loadUint(256))
        .nextWorkchain(cs.loadInt(32).longValue())
        .nextAddrPfx(cs.loadUint(64))
        .importBlockLt(cs.loadUint(64))
        .build();
  }
}
