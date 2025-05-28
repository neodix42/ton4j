package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * processed_upto$_ last_msg_lt:uint64 last_msg_hash:bits256 = ProcessedUpto;
 * </pre>
 */
@Builder
@Data
public class ProcessedUpto implements Serializable {
  BigInteger lastMsgLt;
  BigInteger lastMsgHash;

  public Cell toCell() {
    return CellBuilder.beginCell().storeUint(lastMsgLt, 64).storeUint(lastMsgHash, 64).endCell();
  }

  public static ProcessedUpto deserialize(CellSlice cs) {
    return ProcessedUpto.builder().lastMsgLt(cs.loadUint(64)).lastMsgHash(cs.loadUint(64)).build();
  }
}
