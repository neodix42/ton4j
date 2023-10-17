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
 * msg_import_ext$000 msg:^(Message Any) transaction:^Transaction  = InMsg;
 */
public class InMsgImportExt implements InMsg {
    Message msg;
    Transaction transaction;
}
