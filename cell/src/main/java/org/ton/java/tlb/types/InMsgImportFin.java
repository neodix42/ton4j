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
 * msg_import_fin$100 in_msg:^MsgEnvelope transaction:^Transaction fwd_fee:Grams = InMsg;
 */

// msg_export_new extends InMsg

public class InMsgImportFin implements InMsg {
    MsgEnvelope inMsg;
    Transaction transaction;
    BigInteger fwdFee;
}
