package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;

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
    public Cell toCell();
}
