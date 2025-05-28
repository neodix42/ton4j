package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

public interface MsgEnvelope {
  Cell toCell();

  static MsgEnvelope deserialize(CellSlice cs) {
    int msgEnvelopeFlag = cs.preloadUint(4).intValue();
    switch (msgEnvelopeFlag) {
      case 4:
        {
          return MsgEnvelopeV1.deserialize(cs);
        }
      case 5:
        {
          return MsgEnvelopeV2.deserialize(cs);
        }
    }
    throw new Error("unknown MsgEnvelope flag, found 0x" + Long.toBinaryString(msgEnvelopeFlag));
  }
}
