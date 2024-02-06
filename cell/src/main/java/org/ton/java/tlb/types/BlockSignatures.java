package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

@Builder
@Getter
@Setter
@ToString
/**
 block_signatures#11
 validator_info:ValidatorBaseInfo
 pure_signatures:BlockSignaturesPure
 = BlockSignatures;
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

    public static BlockSignatures deserialize(CellSlice cs) {
        long magic = cs.loadUint(8).longValue();
        assert (magic == 0x11) : "BlockSignatures: magic not equal to 0x11, found 0x" + Long.toHexString(magic);

        return BlockSignatures.builder()
                .magic(0b11)
                .validatorBaseInfo(ValidatorBaseInfo.deserialize(cs))
                .pureSignatures(BlockSignaturesPure.deserialize(cs))
                .build();
    }
}
