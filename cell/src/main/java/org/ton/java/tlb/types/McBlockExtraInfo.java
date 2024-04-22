package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMapE;

import static java.util.Objects.isNull;

@Builder
@Getter
@Setter
@ToString
/**
 * prev_blk_signatures:(HashmapE 16 CryptoSignaturePair)
 * recover_create_msg:(Maybe ^InMsg)
 * mint_msg:(Maybe ^InMsg)
 */
public class McBlockExtraInfo {
    TonHashMapE prevBlkSignatures;
    InMsg recoverCreateMsg;
    InMsg mintMsg;

    public Cell toCell() {
        CellBuilder cell = CellBuilder.beginCell()
                .storeDict(prevBlkSignatures.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 16).endCell().bits,
                        v -> CellBuilder.beginCell().storeCell((Cell) v).endCell() // todo CryptoSignaturePair
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
                .prevBlkSignatures(cs.loadDictE(16,
                        k -> k.readInt(16),
                        v -> v))
                .recoverCreateMsg(cs.loadBit() ? InMsg.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
                .mintMsg(cs.loadBit() ? InMsg.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
                .build();
    }
}
