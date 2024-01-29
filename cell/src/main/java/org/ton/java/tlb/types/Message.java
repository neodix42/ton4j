package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import static java.util.Objects.isNull;

@Builder
@Getter
@Setter
@ToString
/**
 * message$_ {X:Type}
 *   info:CommonMsgInfo
 *   init:(Maybe (Either StateInit ^StateInit))
 *   body:(Either X ^X) = Message X;
 */
public class Message {
    String msgType;
    CommonMsgInfo info;
    StateInit init;
    Cell body;

    public static void dump(InternalMessage intMsg) {
//        return fmt.Sprintf("Amount %s TON, Created at: %d, Created lt %d\nBounce: %t, Bounced %t, IHRDisabled %t\nSrcAddr: %s\nDstAddr: %s\nPayload: %s",
//                m.Amount.TON(), m.CreatedAt, m.CreatedLT, m.Bounce, m.Bounced, m.IHRDisabled, m.SrcAddr, m.DstAddr, m.Body.Dump())
    }

    public Cell toCell() {
        CellBuilder c = CellBuilder.beginCell();

        switch (msgType) {
            case "INTERNAL" -> {
                c.storeSlice(CellSlice.beginParse(((InternalMessage) info.getMsg()).toCell()));
            }
            case "EXTERNAL_IN" -> {
                c.storeSlice(CellSlice.beginParse(((ExternalMessage) info.getMsg()).toCell()));
            }
            case "EXTERNAL_OUT" -> {
                c.storeSlice(CellSlice.beginParse(((ExternalMessageOut) info.getMsg()).toCell()));
            }
        }

        if (isNull(init)) {
            c.storeBit(false);
        } else { // init:(Maybe (Either StateInit ^StateInit))
            c.storeBit(true);
            c.storeBit(true);
            c.storeRef(init.toCell()); // todo check if can be stored in the same cell, not in ref
        }

        if (isNull(body)) {
            c.storeBit(false);
        } else {
            c.storeBit(true);
            c.storeRef(body);
        }
        return c.endCell();
    }
}
