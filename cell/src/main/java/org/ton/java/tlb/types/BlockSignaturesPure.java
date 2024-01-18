package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.TonHashMapE;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 block_signatures_pure#_ sig_count:uint32 sig_weight:uint64
 signatures:(HashmapE 16 CryptoSignaturePair) = BlockSignaturesPure;
 */
public class BlockSignaturesPure {
    long sigCount;
    BigInteger sigWeight;
    TonHashMapE signatures;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(sigCount, 32)
                .storeUint(sigWeight, 64)
                .storeDict(signatures.serialize(
                        k -> CellBuilder.beginCell().storeUint((Long) k, 16).bits,
                        v -> CellBuilder.beginCell().storeCell(((CryptoSignaturePair) v).toCell())
                ))
                .endCell();
    }
}
