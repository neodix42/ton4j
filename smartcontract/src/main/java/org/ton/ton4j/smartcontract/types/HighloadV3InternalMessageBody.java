package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.tlb.*;

/**
 *
 *
 * <pre>
 *     internal_transfer#ae42e5a4
 *     {n:#} query_id:uint64
 *     actions:^(OutList n) = InternalMsgBody n;
 * </pre>
 */
@Builder
@Data
public class HighloadV3InternalMessageBody {

  long magic;
  BigInteger queryId;
  OutList actions;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(BigInteger.valueOf(0xae42e5a4L), 32)
        .storeUint(queryId, 64)
        .storeRef(actions.toCell())
        .endCell();
  }

  public static HighloadV3InternalMessageBody deserialize(CellSlice cs) {
    return HighloadV3InternalMessageBody.builder()
        .magic(cs.loadUint(32).longValue())
        .queryId(cs.loadUint(64))
        .actions(OutList.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
