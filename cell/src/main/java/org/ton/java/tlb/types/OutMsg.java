package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

// msg_export_new extends OutMsg

/**
 * msg_export_ext$000 msg:^(Message Any)
 * transaction:^Transaction = OutMsg;
 * msg_export_imm$010 out_msg:^MsgEnvelope
 * transaction:^Transaction reimport:^InMsg = OutMsg;
 * msg_export_new$001 out_msg:^MsgEnvelope
 * transaction:^Transaction = OutMsg;
 * msg_export_tr$011  out_msg:^MsgEnvelope
 * imported:^InMsg = OutMsg;
 * <p>
 * msg_export_deq$1100 out_msg:^MsgEnvelope
 * import_block_lt:uint63 = OutMsg;
 * <p>
 * msg_export_deq_short$1101 msg_env_hash:bits256
 * next_workchain:int32 next_addr_pfx:uint64
 * import_block_lt:uint64 = OutMsg;
 * <p>
 * msg_export_tr_req$111 out_msg:^MsgEnvelope
 * imported:^InMsg = OutMsg;
 * <p>
 * msg_export_deq_imm$100 out_msg:^MsgEnvelope
 * reimport:^InMsg = OutMsg;
 */
public interface OutMsg {
    Cell toCell();

    static OutMsg deserialize(CellSlice cs) {
        int outMsgFlag = cs.loadUint(3).intValue();
        switch (outMsgFlag) {
            case 0b000: {
                return OutMsgExt.builder()
                        .msg(Message.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .build();
            }
            case 0b010: {
                return OutMsgImm.builder()
                        .msg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .reimport(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .build();
            }
            case 0b001: {
                return OutMsgNew.builder()
                        .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .transaction(Transaction.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .build();
            }
            case 0b011: {
                return OutMsgTr.builder()
                        .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .imported(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .build();
            }
            case 0b111: {
                return OutMsgTrReq.builder()
                        .msg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .imported(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .build();
            }
            case 0b100: {
                return OutMsgDeqImm.builder()
                        .msg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .reimport(InMsg.deserialize(CellSlice.beginParse(cs.loadRef())))
                        .build();
            }
            case 0b110: {
                boolean outMsgSubFlag = cs.loadBit();
                if (outMsgSubFlag) {
                    return OutMsgDeqShort.builder()
                            .msgEnvHash(cs.loadUint(256))
                            .nextWorkchain(cs.loadInt(32).longValue())
                            .nextAddrPfx(cs.loadUint(64))
                            .importBlockLt(cs.loadUint(64))
                            .build();
                } else {
                    return OutMsgDeq.builder()
                            .outMsg(MsgEnvelope.deserialize(CellSlice.beginParse(cs.loadRef())))
                            .importBlockLt(cs.loadUint(63))
                            .build();
                }
            }
        }
        throw new Error("unknown out_msg flag, found 0x" + Long.toBinaryString(outMsgFlag));
    }
}
