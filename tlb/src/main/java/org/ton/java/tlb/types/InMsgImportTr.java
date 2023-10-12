package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * msg_import_tr$101  in_msg:^MsgEnvelope out_msg:^MsgEnvelope transit_fee:Grams = InMsg;
 */
public class InMsgImportTr implements InMsg {
    MsgEnvelope inMsg;
    MsgEnvelope outMsg;
    BigInteger transitFee;
}
