package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

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
public class McBlockExtraInfo implements Serializable {
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
