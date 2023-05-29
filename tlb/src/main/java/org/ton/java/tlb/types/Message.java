package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;

@Builder
@Getter
@Setter
@ToString
public class Message {
    String msgType;//   `tlb:"-"`
    AnyMessage msg;// `tlb:"."`

    public static void dump(InternalMessage intMsg) {
//        return fmt.Sprintf("Amount %s TON, Created at: %d, Created lt %d\nBounce: %t, Bounced %t, IHRDisabled %t\nSrcAddr: %s\nDstAddr: %s\nPayload: %s",
//                m.Amount.TON(), m.CreatedAt, m.CreatedLT, m.Bounce, m.Bounced, m.IHRDisabled, m.SrcAddr, m.DstAddr, m.Body.Dump())
    }

    public static Cell toCell(InternalMessage msg) {
        return null;
    }

    public static Cell toCell(ExternalMessage msg) {
        return null;
    }

    public static Cell toCell(MessagesList msg) {
        return null;
    }
}
