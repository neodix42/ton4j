package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Getter
@Setter
@ToString
/**
 * block_signatures_pure#_
 * sig_count:uint32
 * sig_weight:uint64
 * signatures:(HashmapE 16 CryptoSignaturePair) = BlockSignaturesPure;
 *
 * block_signatures#11
 * validator_info:ValidatorBaseInfo
 * pure_signatures:BlockSignaturesPure = BlockSignatures;
 *
 * block_proof#c3
 * proof_for:BlockIdExt
 * root:^Cell
 * signatures:(Maybe ^BlockSignatures) = BlockProof;
 */
public class BlockProof {
    int magic;
    BlockIdExt proofFor;
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
}
