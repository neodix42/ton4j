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

    public static void dump() {

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
