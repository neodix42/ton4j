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
 * msg_discard_fin$110 in_msg:^MsgEnvelope transaction_id:uint64 fwd_fee:Grams = InMsg;
 */
public class InMsgDiscardFin implements InMsg {
    MsgEnvelope inMsg;
    BigInteger transactionId;
    BigInteger fwdFee;
}
