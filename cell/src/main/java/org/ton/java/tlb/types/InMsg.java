package org.ton.java.tlb.types;

import org.ton.java.cell.Cell;

/**
 * msg_import_ext$000 msg:^(Message Any) transaction:^Transaction
 * = InMsg;
 * msg_import_ihr$010 msg:^(Message Any) transaction:^Transaction
 * ihr_fee:Grams proof_created:^Cell = InMsg;
 * msg_import_imm$011 in_msg:^MsgEnvelope
 * transaction:^Transaction fwd_fee:Grams = InMsg;
 * msg_import_fin$100 in_msg:^MsgEnvelope
 * transaction:^Transaction fwd_fee:Grams = InMsg;
 * msg_import_tr$101  in_msg:^MsgEnvelope out_msg:^MsgEnvelope
 * transit_fee:Grams = InMsg;
 * msg_discard_fin$110 in_msg:^MsgEnvelope transaction_id:uint64
 * fwd_fee:Grams = InMsg;
 * msg_discard_tr$111 in_msg:^MsgEnvelope transaction_id:uint64
 * fwd_fee:Grams proof_delivered:^Cell = InMsg;
 */

// msg_export_new extends InMsg

public interface InMsg {

    public Cell toCell();
}
