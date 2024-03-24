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
    CommonMsgInfo info;
    StateInit init;
    Cell body;

    public static void dump(InternalMessage intMsg) {
//        return fmt.Sprintf("Amount %s TON, Created at: %d, Created lt %d\nBounce: %t, Bounced %t, IHRDisabled %t\nSrcAddr: %s\nDstAddr: %s\nPayload: %s",
//                m.Amount.TON(), m.CreatedAt, m.CreatedLT, m.Bounce, m.Bounced, m.IHRDisabled, m.SrcAddr, m.DstAddr, m.Body.Dump())
    }

    public Cell toCell() {
        CellBuilder c = CellBuilder.beginCell();

        c.storeCell(info.toCell());

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

    public static Message deserialize(CellSlice cs) {
        CommonMsgInfo commonMsgInfo = CommonMsgInfo.deserialize(cs);
        return Message.builder()
                .info(commonMsgInfo)
                .init(cs.loadBit() ?
                        (cs.loadBit() ?
                                StateInit.deserialize(CellSlice.beginParse(cs.loadRef()))
                                : StateInit.deserialize(cs))
                        : StateInit.builder().build())
                .body(cs.loadBit() ?
                        cs.loadRef() : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits())))
                .build();
    }
}
