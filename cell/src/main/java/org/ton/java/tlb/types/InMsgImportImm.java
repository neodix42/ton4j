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
 * msg_import_imm$011 in_msg:^MsgEnvelope transaction:^Transaction fwd_fee:Grams = InMsg;
 */
public class InMsgImportImm implements InMsg {
    MsgEnvelope inMsg;
    Transaction transaction;
    BigInteger fwdFee;
    Cell proofCreated;
}
