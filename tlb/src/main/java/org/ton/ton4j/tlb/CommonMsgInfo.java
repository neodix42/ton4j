package org.ton.ton4j.tlb;

import java.math.BigInteger;
import java.util.HashMap;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
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
 *
 * ext_in_msg_info$10
 * src:MsgAddressExt
 * dest:MsgAddressInt
 * import_fee:Grams = CommonMsgInfo;
 *
 * ext_out_msg_info$11
 * src:MsgAddressInt
 * dest:MsgAddressExt
 * created_lt:uint64
 * created_at:uint32 = CommonMsgInfo;
 * </pre>
 */
public interface CommonMsgInfo {

  Cell toCell();

  static CommonMsgInfo deserialize(CellSlice cs) {
    boolean isExternal = cs.preloadBit();
    if (!isExternal) {
      return InternalMessageInfo.deserialize(cs);
    } else {
      boolean isOut = cs.preloadBitAt(2);
      if (isOut) {
        return ExternalMessageOutInfo.deserialize(cs);
      } else {
        return ExternalMessageInInfo.deserialize(cs);
      }
    }
  }

  String getType();

  String getSourceAddress();

  String getDestinationAddress();

  BigInteger getValueCoins();

  HashMap getExtraCurrencies();
}
