package org.ton.java.tlb.types;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;

import java.math.BigInteger;

@Builder
@Getter
@Setter
@ToString
/**
 * validator#53 public_key:SigPubKey weight:uint64 = ValidatorDescr;
 */
public class Validator implements ValidatorDescr {
    long magic;
    //SigPubKeyED25519 publicKey;
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
