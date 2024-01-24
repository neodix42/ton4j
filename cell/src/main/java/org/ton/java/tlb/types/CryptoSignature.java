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
 ed25519_signature#5 R:bits256 s:bits256 = CryptoSignatureSimple;  // 516 bits
 _ CryptoSignatureSimple = CryptoSignature; */
public class CryptoSignature {
    int magic;
    BigInteger r;
    BigInteger s;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x5, 8)
                .storeUint(r, 256)
                .storeUint(s, 256)
                .endCell();
    }
}
