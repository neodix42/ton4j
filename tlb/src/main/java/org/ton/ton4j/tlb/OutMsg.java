package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_export_ext$000 msg:^(Message Any) transaction:^Transaction = OutMsg;
 * msg_export_imm$010 out_msg:^MsgEnvelope transaction:^Transaction reimport:^InMsg = OutMsg;
 * msg_export_new$001 out_msg:^MsgEnvelope transaction:^Transaction = OutMsg;
 * msg_export_tr$011  out_msg:^MsgEnvelope imported:^InMsg = OutMsg;
 * msg_export_deq$1100 out_msg:^MsgEnvelope import_block_lt:uint63 = OutMsg;
 * msg_export_deq_short$1101 msg_env_hash:bits256 next_workchain:int32 next_addr_pfx:uint64 import_block_lt:uint64 = OutMsg;
 * msg_export_tr_req$111 out_msg:^MsgEnvelope imported:^InMsg = OutMsg;
 * msg_export_deq_imm$100 out_msg:^MsgEnvelope reimport:^InMsg = OutMsg;
 * </pre>
 */
public interface OutMsg {
  Cell toCell();

  static OutMsg deserialize(CellSlice cs) {

    int outMsgFlag = cs.preloadUint(3).intValue();
    switch (outMsgFlag) {
      case 0b000:
        {
          return OutMsgExt.deserialize(cs);
        }
      case 0b010:
        {
          return OutMsgImm.deserialize(cs);
        }
      case 0b001:
        {
          return OutMsgNew.deserialize(cs);
        }
      case 0b011:
        {
          return OutMsgTr.deserialize(cs);
        }
      case 0b111:
        {
          return OutMsgTrReq.deserialize(cs);
        }
      case 0b100:
        {
          return OutMsgDeqImm.deserialize(cs);
        }
      case 0b110:
        {
          boolean outMsgSubFlag = cs.preloadBitAt(4);
          if (outMsgSubFlag) {
            return OutMsgDeqShort.deserialize(cs);
          } else {
            return OutMsgDeq.deserialize(cs);
          }
        }
      case 0b101:
        {
          boolean outMsgSubFlag = cs.preloadBitAt(5);
          if (!outMsgSubFlag) {
            return OutMsgNewDefer.builder()
                .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
          } else {
            return OutMsgDeferredTr.builder()
                .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                .imported(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
                .build();
          }
        }
    }
    throw new Error("unknown out_msg flag, found 0x" + Long.toBinaryString(outMsgFlag));
  }
}
