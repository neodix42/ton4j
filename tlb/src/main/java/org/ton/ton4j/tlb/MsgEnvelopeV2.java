package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

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
 * msg_envelope_v2#5
 * cur_addr:IntermediateAddress
 * next_addr:IntermediateAddress
 * fwd_fee_remaining:Grams
 * msg:^(Message Any)
 * emitted_lt:(Maybe uint64)
 * metadata:(Maybe MsgMetadata) = MsgEnvelope;
 *
 * }</pre>
 */
@Builder
@Data
public class MsgEnvelopeV2 implements MsgEnvelope, Serializable {
  int magic;
  IntermediateAddress currAddr;
  IntermediateAddress nextAddr;
  BigInteger fwdFeeRemaining;
  Message msg;
  BigInteger emittedLt;
  MsgMetaData msgMetaData;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(5, 4) // magic
        .storeCell(currAddr.toCell())
        .storeCell(nextAddr.toCell())
        .storeCoins(fwdFeeRemaining)
        .storeRef(msg.toCell())
        .storeUintMaybe(emittedLt, 64)
        .storeCellMaybe(isNull(msgMetaData) ? null : msgMetaData.toCell())
        .endCell();
  }

  public static MsgEnvelopeV2 deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).longValue();
    assert (magic == 5) : "MsgEnvelopeV2: magic not equal to 5, found 0x" + Long.toHexString(magic);

    return MsgEnvelopeV2.builder()
        .currAddr(IntermediateAddress.deserialize(cs))
        .nextAddr(IntermediateAddress.deserialize(cs))
        .fwdFeeRemaining(cs.loadCoins())
        .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
        .emittedLt(cs.loadBit() ? cs.loadUint(64) : null)
        .msgMetaData(cs.loadBit() ? MsgMetaData.deserialize(cs) : null)
        .build();
  }
}
