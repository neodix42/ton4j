package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * msg_import_tr$101  in_msg:^MsgEnvelope out_msg:^MsgEnvelope  transit_fee:Grams = InMsg;
 */
public class InMsgDiscardTr implements InMsg {
    MsgEnvelope inMsg;
    BigInteger transactionId;
    BigInteger fwdFee;
    Cell proofDelivered;
}
