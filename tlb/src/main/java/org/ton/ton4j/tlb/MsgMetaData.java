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
 * <pre>{@code
 * msg_metadata#0 depth:uint32 initiator_addr:MsgAddressInt initiator_lt:uint64 = MsgMetadata;
 *
 *
 * }</pre>
 */
@Builder
@Data
public class MsgMetaData implements MsgEnvelope, Serializable {
  int magic;
  long depth;
  MsgAddressInt initiatorAddress;
  BigInteger initiatorLt;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0, 4)
        .storeUint(depth, 32)
        .storeCell(initiatorAddress.toCell())
        .storeUint(initiatorLt, 64)
        .endCell();
  }

  public static MsgMetaData deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).longValue();
    assert (magic == 0) : "MsgMetaData: magic not equal to 0, found 0x" + Long.toHexString(magic);

    return MsgMetaData.builder()
        .depth(cs.loadUint(32).longValue())
        .initiatorAddress(
            MsgAddressInt.deserialize(cs)) // there are blocks with ext address though, todo
        .initiatorLt(cs.loadUint(64))
        .build();
  }
}
