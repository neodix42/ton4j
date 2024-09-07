package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

/**
 * <pre>
 * ed25519_pubkey#8e81278a pubkey:bits256 = SigPubKey;  // 288 bits
 * </pre>
 */
@Builder
@Getter
@Setter
@ToString

public class SigPubKey {
    long magic;
    BigInteger pubkey;

    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x8e81278a, 32)
                .storeUint(pubkey, 256)
                .endCell();
    }

    public static SigPubKey deserialize(CellSlice cs) {
        return SigPubKey.builder()
                .magic(cs.loadUint(32).longValue())
                .pubkey(cs.loadUint(256))
                .build();
    }
}
