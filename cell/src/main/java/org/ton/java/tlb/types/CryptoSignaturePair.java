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
/**
 sig_pair$_ node_id_short:bits256 sign:CryptoSignature = CryptoSignaturePair; // 256+x ~ 772 bits
 */
public class CryptoSignaturePair {
    BigInteger nodeIdShort;
    CryptoSignature sign;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(nodeIdShort, 256)
                .storeCell(sign.toCell())
                .endCell();
    }
}
