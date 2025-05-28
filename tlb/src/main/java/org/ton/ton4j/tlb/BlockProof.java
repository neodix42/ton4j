package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

/**
 *
 *
 * <pre>
 * block_proof#c3
 * proof_for:BlockIdExt
 * root:^Cell
 * signatures:(Maybe ^BlockSignatures) = BlockProof;
 * </pre>
 */
@Builder
@Data
public class BlockProof implements Serializable {
  int magic;
  BlockIdExtShardIdent proofFor;
  Cell root;
  BlockSignatures signatures;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0xc3, 8)
        .storeCell(proofFor.toCell())
        .storeRef(root)
        .storeRefMaybe(signatures.toCell())
        .endCell();
  }

  public static BlockProof deserialize(CellSlice cs) {
    long magic = cs.loadUint(8).longValue();
    assert (magic == 0xc3)
        : "BlockProof: magic not equal to 0xc3, found 0x" + Long.toHexString(magic);

    return BlockProof.builder()
        .magic(0xc3)
        .proofFor(BlockIdExtShardIdent.deserialize(cs))
        .root(cs.loadRef())
        .signatures(
            cs.loadBit() ? BlockSignatures.deserialize(CellSlice.beginParse(cs.loadRef())) : null)
        .build();
  }
}
