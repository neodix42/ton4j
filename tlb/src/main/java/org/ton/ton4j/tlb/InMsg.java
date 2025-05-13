package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * msg_import_ext$000 msg:^(Message Any)  transaction:^Transaction = InMsg;
 * msg_import_ihr$010 msg:^(Message Any)  transaction:^Transaction ihr_fee:Grams proof_created:^Cell = InMsg;
 * msg_import_imm$011 in_msg:^MsgEnvelope transaction:^Transaction fwd_fee:Grams = InMsg;
 * msg_import_fin$100 in_msg:^MsgEnvelope transaction:^Transaction fwd_fee:Grams = InMsg;
 * msg_import_tr$101  in_msg:^MsgEnvelope out_msg:^MsgEnvelope transit_fee:Grams = InMsg;
 * msg_discard_fin$110 in_msg:^MsgEnvelope transaction_id:uint64 fwd_fee:Grams = InMsg;
 * msg_discard_tr$111 in_msg:^MsgEnvelope transaction_id:uint64 fwd_fee:Grams proof_delivered:^Cell = InMsg;
 * </pre>
 */

// msg_export_new extends InMsg
public interface InMsg {

  Cell toCell();

  static InMsg deserialize(CellSlice cs) {
    int inMsgFlag = cs.loadUint(3).intValue();
    switch (inMsgFlag) {
      case 0b000:
        {
          return InMsgImportExt.builder()
              .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
              .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
              .build();
        }
      case 0b010:
        {
          return InMsgImportIhr.builder()
              .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
              .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
              .ihrFee(cs.loadCoins())
              .proofCreated(cs.loadRef())
              .build();
        }
      case 0b011:
        {
          return InMsgImportImm.builder()
              .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
              .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
              .fwdFee(cs.loadCoins())
              .build();
        }
      case 0b100:
        {
          return InMsgImportFin.builder()
              .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
              .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
              .fwdFee(cs.loadCoins())
              .build();
        }
      case 0b101:
        {
          return InMsgImportTr.builder()
              .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
              .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
              .transitFee(cs.loadCoins())
              .build();
        }
      case 0b110:
        {
          return InMsgDiscardFin.builder()
              .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
              .transactionId(cs.loadUint(64))
              .fwdFee(cs.loadCoins())
              .build();
        }
      case 0b111:
        {
          return InMsgDiscardTr.builder()
              .inMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
              .transactionId(cs.loadUint(64))
              .fwdFee(cs.loadCoins())
              .proofDelivered(cs.loadRef())
              .build();
        }
    }
    throw new Error("unknown in_msg flag, found 0x" + Long.toBinaryString(inMsgFlag));
  }
}
