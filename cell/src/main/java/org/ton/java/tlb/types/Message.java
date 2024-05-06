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
 *   init:(Maybe (Either StateInit ^StateInit)) - default storeBit(false)
 *   body:(Either X ^X)  - default storeBit(false)
 *   = Message X;
 */
public class Message {
    CommonMsgInfo info;
    StateInit init;
    Cell body;

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
            if ((c.getFreeBits() >= c.getUsedBits()) && c.getFreeRefs() >= c.getUsedRefs()) {
                c.storeBit(false);
                c.storeCell(body);
            } else {
                c.storeBit(true);
                c.storeRef(body);
            }
        }
        return c.endCell();
    }

    public static Message deserialize(CellSlice cs) {
        return Message.builder()
                .info(CommonMsgInfo.deserialize(cs))
                .init(cs.loadBit() ?
                        (cs.loadBit() ?
                                StateInit.deserialize(CellSlice.beginParse(cs.loadRef()))
                                : StateInit.deserialize(cs))
                        : StateInit.builder().build())
                .body(cs.loadBit() ?
                        cs.loadRef() : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits())).endCell())
                .build();
    }
}
