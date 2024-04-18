package org.ton.java.tlb.types;


import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;

/**
 * int_msg_info$0
 * ihr_disabled:Bool
 * bounce:Bool
 * bounced:Bool
 * src:MsgAddressInt
 * dest:MsgAddressInt
 * value:CurrencyCollection
 * ihr_fee:Grams
 * fwd_fee:Grams
 * created_lt:uint64
 * created_at:uint32 = CommonMsgInfo;
 * <p>
 * ext_in_msg_info$10
 * src:MsgAddressExt
 * dest:MsgAddressInt
 * import_fee:Grams = CommonMsgInfo;
 * <p>
 * ext_out_msg_info$11
 * src:MsgAddressInt
 * dest:MsgAddressExt
 * created_lt:uint64
 * created_at:uint32 = CommonMsgInfo;
 */

public interface CommonMsgInfo {

    Cell toCell();

    static CommonMsgInfo deserialize(CellSlice cs) {
        boolean isExternal = cs.preloadBit();
        if (!isExternal) {
            return InternalMessage.deserialize(cs);
        } else {
            boolean isOut = cs.preloadBitAt(2);
            if (isOut) {
                return ExternalMessageOut.deserialize(cs);
            } else {
                return ExternalMessage.deserialize(cs);
            }
        }
    }
}
