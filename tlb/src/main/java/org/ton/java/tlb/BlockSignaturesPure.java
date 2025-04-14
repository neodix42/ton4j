package org.ton.java.tlb;

import java.io.Serializable;
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
 * block_signatures_pure#_ sig_count:uint32 sig_weight:uint64
 * signatures:(HashmapE 16 CryptoSignaturePair) = BlockSignaturesPure;
 * </pre>
 */
@Builder
@Data
public class BlockSignaturesPure implements Serializable {
  long sigCount;
  BigInteger sigWeight;
  TonHashMapE signatures;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(sigCount, 32)
        .storeUint(sigWeight, 64)
        .storeDict(
            signatures.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 16).endCell().getBits(),
                v ->
                    CellBuilder.beginCell()
                        .storeCell(((CryptoSignaturePair) v).toCell())
                        .endCell()))
        .endCell();
  }

  public static BlockSignaturesPure deserialize(CellSlice cs) {
    return BlockSignaturesPure.builder()
        .sigCount(cs.loadUint(32).longValue())
        .sigWeight(cs.loadUint(64))
        .signatures(
            cs.loadDictE(
                16,
                k -> k.readUint(16),
                v -> CryptoSignaturePair.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
