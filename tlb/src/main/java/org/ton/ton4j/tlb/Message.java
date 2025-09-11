package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.utils.Utils;

/**
 *
 *
 * <pre>
 * message$_ {X:Type}
 *   info:CommonMsgInfo
 *   init:(Maybe (Either StateInit ^StateInit)) - default storeBit(false)
 *   body:(Either X ^X) - default storeBit(false)
 *   = Message X;
 * </pre>
 */
@Builder
@Data
public class Message implements Serializable {
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
    if (cs.isExotic()) {
      return Message.builder().build();
    }
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

  public byte[] getNormalizedHash() {
    return CellBuilder.beginCell()
        .storeUint(0b10, 2)
        .storeAddress(null)
        .storeAddress(Address.of(info.getDestinationAddress()))
        .storeCoins(BigInteger.ZERO)
        .storeBit(false) // no import fee
        .storeBit(true) // no state init
        .storeRef( // body always in ref
            isNull(body)
                ? CellBuilder.beginCell().endCell()
                : CellBuilder.beginCell().storeCell(body).endCell())
        .endCell()
        .getHash();
  }

  public String getComment() {
    if (nonNull(body)) {
      CellSlice cs = CellSlice.beginParse(body);
      if (cs.preloadUint(32).longValue() == 0) {
        if (cs.getRefsCount() == 1) {
          cs.loadUint(32);
          return cs.loadSnakeString();
        } else {
          cs.loadUint(32);
          return cs.loadString(cs.getRestBits());
        }
      }
      return Utils.bytesToHex(cs.loadBytes());
    } else {
      return "";
    }
  }
}
