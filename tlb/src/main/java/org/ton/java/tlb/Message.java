package org.ton.java.tlb;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

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
      c.storeBit(false);
    } else { // init:(Maybe (Either StateInit ^StateInit))
      c.storeBit(true);
      Cell initCell = init.toCell();
      if ((c.getFreeBits() - 2
          >= (initCell.getBits().getUsedBits())
              + (nonNull(body) ? body.getBits().getUsedBits() : 0))) {
        c.storeBit(false);
        c.storeCell(initCell);
      } else {
        c.storeBit(true);
        c.storeRef(initCell);
      }
    }

    if (isNull(body)) {
      c.storeBit(false);
    } else {
      if ((c.getFreeBits() >= body.getBits().getUsedBits())
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
    Message message = Message.builder().info(CommonMsgInfo.deserialize(cs)).build();

    StateInit stateInit = null;
    if (cs.loadBit()) {
      if (cs.loadBit()) { // load from ref
        stateInit = StateInit.deserialize(CellSlice.beginParse(cs.loadRef()));
      } else { // load from slice
        stateInit = StateInit.deserialize(cs);
      }
    }
    message.setInit(stateInit);

    Cell body;
    if (cs.loadBit()) {
      body = cs.loadRef();
    } else {
      body = cs.sliceToCell();
      cs.loadBits(cs.getRestBits());
    }

    message.setBody(body);
    return message;
  }
}
