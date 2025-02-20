package org.ton.java.tlb;

import static java.util.Objects.isNull;

import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 *
 *
 * <pre>
 * message$_ {X:Type}
 *   info:CommonMsgInfo
 *   init:(Maybe (Either StateInit ^StateInit)) - default storeBit(false)
 *   body:(Either X ^X)  - default storeBit(false)
 *   = Message X;
 *   </pre>
 */
@Builder
@Data
public class Message {
  CommonMsgInfo info;
  StateInit init;
  Cell body;

  public Cell toCell() {
    CellBuilder c = CellBuilder.beginCell();
    c.storeCell(info.toCell());

    if (isNull(init)) {
      c.storeBit(false); // maybe false
    } else { // init:(Maybe (Either StateInit ^StateInit))
      c.storeBit(true); // maybe true
      Cell initCell = init.toCell();
      // -1:  need at least one bit for body
      if ((c.getFreeBits() - 2 >= initCell.getBits().getUsedBits())
          && (c.getFreeRefs() >= initCell.getRefs().size())) {
        c.storeBit(false); // Either left
        c.storeCell(initCell);
      } else {
        c.storeBit(true); // Either right
        c.storeRef(initCell);
      }
    }

    if (isNull(body)) {
      c.storeBit(false);
    } else {
      if ((c.getFreeBits() - 1 >= body.getBits().getUsedBits())
          && c.getFreeRefs() >= body.getUsedRefs()) {
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
        .init(
            cs.loadBit()
                ? (cs.loadBit()
                    ? StateInit.deserialize(CellSlice.beginParse(cs.loadRef()))
                    : StateInit.deserialize(cs))
                : null)
        .body(
            cs.loadBit()
                ? cs.loadRef()
                : CellBuilder.beginCell().storeBitString(cs.loadBits(cs.getRestBits())).endCell())
        .build();
  }
}
