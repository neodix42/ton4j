package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * message$_ {X:Type}
 *   info:CommonMsgInfoRelaxed
 *   init:(Maybe (Either StateInit ^StateInit)) - default storeBit(false)
 *   body:(Either X ^X) - default storeBit(false)
 *   = MessageRelaxed X;
 * </pre>
 */
@Builder
@Data
public class MessageRelaxed {
  CommonMsgInfoRelaxed info;
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

  public static MessageRelaxed deserialize(CellSlice cs) {
    MessageRelaxed message =
        MessageRelaxed.builder().info(CommonMsgInfoRelaxed.deserialize(cs)).build();
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
