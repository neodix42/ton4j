package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
public class HashUpdate {
    int magic; //  `tlb:"#72"`
    BigInteger oldHash; // `tlb:"bits 256"`
    BigInteger newHash; // `tlb:"bits 256"`

    private String getMagic() {
        return Long.toHexString(magic);
    }

    private String getOldHash() {
        return oldHash.toString(16);
    }

    private String getNewHash() {
        return newHash.toString(16);
    }

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x72, 32)
                .storeUint(oldHash, 256)
                .storeUint(newHash, 256).endCell();
    }
}
