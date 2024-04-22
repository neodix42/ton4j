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
 message$_ {X:Type}
 info:CommonMsgInfoRelaxed
 init:(Maybe (Either StateInit ^StateInit))
 body:(Either X ^X) = MessageRelaxed X;
 */
public class MessageRelaxed {
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
            c.storeBit(true);
            c.storeRef(body);
        }
        return c.endCell();
    }

    public static MessageRelaxed deserialize(CellSlice cs) {
        return MessageRelaxed.builder()
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
