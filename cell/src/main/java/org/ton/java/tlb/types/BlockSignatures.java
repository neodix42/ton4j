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
 block_signatures_pure#_ sig_count:uint32 sig_weight:uint64
 signatures:(HashmapE 16 CryptoSignaturePair) = BlockSignaturesPure;
 block_signatures#11 validator_info:ValidatorBaseInfo pure_signatures:BlockSignaturesPure = BlockSignatures;
 */
public class BlockSignatures {
    int magic;
    ValidatorBaseInfo validatorBaseInfo;
    BlockSignaturesPure pureSignatures;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x11, 8)
                .storeCell(validatorBaseInfo.toCell())
                .storeCell(pureSignatures.toCell())
                .endCell();
    }
}
