package org.ton.java.tlb;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * prev_blk_signatures:(HashmapE 16 CryptoSignaturePair)
 * recover_create_msg:(Maybe ^InMsg)
 * mint_msg:(Maybe ^InMsg)
 * </pre>
 */
@Builder
@Data
public class McBlockExtraInfo {
  TonHashMapE prevBlkSignatures;
  InMsg recoverCreateMsg;
  InMsg mintMsg;

  public Cell toCell() {
    CellBuilder cell =
        CellBuilder.beginCell()
            .storeDict(
                prevBlkSignatures.serialize(
                    k -> CellBuilder.beginCell().storeUint((BigInteger) k, 16).endCell().getBits(),
                    v ->
                        CellBuilder.beginCell()
                            .storeCell((Cell) v)
                            .endCell() // todo CryptoSignaturePair
                    ));
    if (isNull(recoverCreateMsg)) {
      cell.storeBit(false);
    } else {
      cell.storeBit(true);
      cell.storeRef(recoverCreateMsg.toCell());
    }

    if (isNull(mintMsg)) {
      cell.storeBit(false);
    } else {
      cell.storeBit(true);
      cell.storeRef(mintMsg.toCell());
    }

    return cell.endCell();
  }

  public static McBlockExtraInfo deserialize(CellSlice cs) {
    return McBlockExtraInfo.builder()
        .prevBlkSignatures(cs.loadDictE(16, k -> k.readInt(16), v -> v))
        .recoverCreateMsg(
            cs.loadBit() ? InMsg.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
        .mintMsg(cs.loadBit() ? InMsg.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
        .build();
  }
}
