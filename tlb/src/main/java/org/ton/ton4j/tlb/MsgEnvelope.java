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
 * interm_addr_regular$0 use_dest_bits:(#<= 96) = IntermediateAddress;
 * interm_addr_simple$10 workchain_id:int8 addr_pfx:uint64 = IntermediateAddress;
 * interm_addr_ext$11 workchain_id:int32 addr_pfx:uint64 = IntermediateAddress;
 *
 * msg_envelope#4
 *   cur_addr:IntermediateAddress
 *   next_addr:IntermediateAddress
 *   fwd_fee_remaining:Grams
 *   msg:^(Message Any) = MsgEnvelope;
 *
 * }</pre>
 */
@Builder
@Data
public class MsgEnvelope implements Serializable {
  int magic;
  IntermediateAddress currAddr;
  IntermediateAddress nextAddr;
  BigInteger fwdFeeRemaining;
  Message msg;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(4, 4) // magic
        .storeCell(currAddr.toCell())
        .storeCell(nextAddr.toCell())
        .storeCoins(fwdFeeRemaining)
        .storeRef(msg.toCell())
        .endCell();
  }

  public static MsgEnvelope deserialize(CellSlice cs) {
    long magic = cs.loadUint(4).longValue();
    assert (magic == 4) : "MsgEnvelope: magic not equal to 4, found 0x" + Long.toHexString(magic);

    return MsgEnvelope.builder()
        .currAddr(IntermediateAddress.deserialize(cs))
        .nextAddr(IntermediateAddress.deserialize(cs))
        .fwdFeeRemaining(cs.loadCoins())
        .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
        .build();
  }
}
