package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Getter
@Setter
@ToString
/**
 * int_msg_info$0
 *   ihr_disabled:Bool
 *   bounce:Bool
 *   bounced:Bool
 *   src:MsgAddressInt
 *   dest:MsgAddressInt
 *   value:CurrencyCollection
 *   ihr_fee:Grams
 *   fwd_fee:Grams
 *   created_lt:uint64
 *   created_at:uint32 = CommonMsgInfo;
 *
 * ext_in_msg_info$10
 *   src:MsgAddressExt
 *   dest:MsgAddressInt
 *   import_fee:Grams = CommonMsgInfo;
 *
 * ext_out_msg_info$11
 *   src:MsgAddressInt
 *   dest:MsgAddressExt
 *   created_lt:uint64
 *   created_at:uint32 = CommonMsgInfo;
 */

public class CommonMsgInfo {
    int magic;

}
