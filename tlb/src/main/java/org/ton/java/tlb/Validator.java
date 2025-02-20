package org.ton.java.tlb;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

/**
 * validator#53 public_key:SigPubKey weight:uint64 = ValidatorDescr;
 */
@Builder
@Data
public class Validator implements ValidatorDescr {
    long magic;
    SigPubKey publicKey;
    BigInteger weight;

    @Override
    public Cell toCell() {
        return CellBuilder.beginCell()
                .storeUint(0x53, 8)
                .storeCell(publicKey.toCell())
                .storeUint(weight, 64)
                .endCell();
    }

    public static ValidatorAddr deserialize(CellSlice cs) {
        return ValidatorAddr.builder()
                .magic(cs.loadUint(8).intValue())
                .publicKey(SigPubKey.deserialize(cs))
                .weight(cs.loadUint(64))
                .build();
    }
}
