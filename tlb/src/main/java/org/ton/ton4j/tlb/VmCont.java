package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface VmCont {

  Cell toCell();

  static VmCont deserialize(CellSlice cs) {
    CellSlice c = cs.clone();
    int magic = c.preloadUint(2).intValue();
    if (magic == 0b00) {
      return VmcStd.deserialize(cs);
    } else if (magic == 0b01) {
      return VmcEnvelope.deserialize(cs);
    } else if (magic == 0b10) {
      int magic2 = c.skipBits(2).preloadUint(2).intValue();
      if (magic2 == 0b00) {
        return VmcQuit.deserialize(cs);
      } else if (magic2 == 0b01) {
        return VmcQuitExc.deserialize(cs);
      } else if (magic2 == 0b10) {
        return VmcQuitExc.deserialize(cs);
      } else {
        throw new Error("Error deserializing VmCont, wrong magic " + magic2);
      }
    } else if (magic == 0b11) {
      int magic2 = c.skipBits(2).preloadUint(4).intValue();
      if (magic2 == 0b0000) {
        return VmcUntil.deserialize(cs);
      } else if (magic2 == 0b0001) {
        return VmcAgain.deserialize(cs);
      } else if (magic2 == 0b0010) {
        return VmcWhileCond.deserialize(cs);
      } else if (magic2 == 0b0011) {
        return VmcWhileBody.deserialize(cs);
      } else {
        throw new Error("Error deserializing VmCont, wrong magic " + magic2);
      }

    } else {
      throw new Error("Error deserializing VmStackValue, wrong magic " + magic);
    }
  }
}
