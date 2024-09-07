package org.ton.java.tlb.types;


import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

/**
 * <pre>
 * int_msg_info$0 ihr_disabled:Bool bounce:Bool bounced:Bool
 * src:MsgAddress
 * dest:MsgAddressInt
 * value:CurrencyCollection
 * ihr_fee:Grams
 * fwd_fee:Grams
 * created_lt:uint64
 * created_at:uint32 = CommonMsgInfoRelaxed;
 *
 * ext_out_msg_info$11 src:MsgAddress dest:MsgAddressExt created_lt:uint64 created_at:uint32 = CommonMsgInfoRelaxed;
 * </pre>
 */


public interface CommonMsgInfoRelaxed {
    Cell toCell();

    static CommonMsgInfoRelaxed deserialize(CellSlice cs) {
        boolean magic = cs.preloadBit();
        if (!magic) {
            return InternalMessageInfoRelaxed.deserialize(cs);
        } else {
            return ExternalMessageOutInfoRelaxed.deserialize(cs);
        }
    }
}
