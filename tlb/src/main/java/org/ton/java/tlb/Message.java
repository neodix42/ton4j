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

  Boolean forceStateInitRef;
  Boolean forceBodyRef;

  public Cell toCell() {
    boolean needRef;
    CellBuilder c = CellBuilder.beginCell();
    c.storeCell(info.toCell());

    if (isNull(init)) {
      c.storeBit(false); // maybe false
    } else { // init:(Maybe (Either StateInit ^StateInit))
      c.storeBit(true); // maybe true
      Cell initCell = init.toCell();
      needRef = false;
      if (nonNull(forceStateInitRef) && forceStateInitRef) {
        needRef = true;
      } else if (c.getFreeBits() - 2
          < (initCell.getBits().getUsedBits()
              + (isNull(body) ? 0 : body.getBits().getUsedBits()))) {
        needRef = false;
      }
      if (needRef) {
        c.storeBit(true);
        c.storeRef(initCell);
      } else {
        c.storeBit(false);
        c.storeCell(initCell);
      }
    }

    if (isNull(body)) {
      c.storeBit(false);
    } else {
      needRef = false;

      if (nonNull(forceBodyRef) && forceBodyRef) {
        needRef = true;
      } else if ((c.getFreeBits() - 1 < body.getBits().getUsedBits())
          || (c.getFreeRefs() + body.getUsedRefs() > 4)) {
        needRef = false;
      }

      if (needRef) {
        c.storeBit(true);
        c.storeRef(body);
      } else {
        c.storeBit(false);
        c.storeCell(body);
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
        message.setForceStateInitRef(true); // remember, so we could serialize back the same way
      } else { // load from slice
        stateInit = StateInit.deserialize(cs);
        message.setForceStateInitRef(false);
      }
    }
    message.setInit(stateInit);

    Cell body;
    if (cs.loadBit()) {
      body = cs.loadRef();
      message.setForceBodyRef(true);
    } else {
      body = cs.sliceToCell();
      cs.loadBits(cs.getRestBits());
      message.setForceBodyRef(false);
    }

    message.setBody(body);
    return message;
  }
}
