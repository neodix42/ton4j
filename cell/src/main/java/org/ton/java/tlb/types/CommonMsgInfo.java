package org.ton.java.tlb.types;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
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

@ToString
@Builder
@Getter
@Setter
public class CommonMsgInfo {
    String msgType;
    CommonMsg msg;

    public static CommonMsgInfo deserialize(CellSlice cs) {
//        if (isNull(cs)) {
//            return Message.builder().build();
//        }
        boolean isExternal = cs.preloadBit();
        if (!isExternal) {
            InternalMessage internalMessage = InternalMessage.deserialize(cs);

            return CommonMsgInfo.builder()
                    .msgType("INTERNAL")
                    .msg(internalMessage)
                    .build();
        } else {
            boolean isOut = cs.preloadBitAt(2);
            if (isOut) {
                ExternalMessageOut externalMessageOut = ExternalMessageOut.deserialize(cs);
                return CommonMsgInfo.builder()
                        .msgType("EXTERNAL_OUT")
                        .msg(externalMessageOut)
                        .build();
            } else {
                ExternalMessage externalMessage = ExternalMessage.deserialize(cs);
                return CommonMsgInfo.builder()
                        .msgType("EXTERNAL_IN")
                        .msg(externalMessage)
                        .build();
            }
        }
        //throw new Error("Unknown msg type ");
    }
}
