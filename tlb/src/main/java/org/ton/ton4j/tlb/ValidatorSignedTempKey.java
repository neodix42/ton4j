package org.ton.ton4j.tlb;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

import java.io.Serializable;

@Builder
@Data
public class ValidatorSignedTempKey implements Serializable {
  int magic;
  ValidatorTempKey key;
  CryptoSignature signature;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x4, 4)
        .storeRef(key.toCell())
        .storeCell(signature.toCell())
        .endCell();
  }

  public static ValidatorSignedTempKey deserialize(CellSlice cs) {
    return ValidatorSignedTempKey.builder()
        .magic(cs.loadUint(4).intValue())
        .key(ValidatorTempKey.deserialize(CellSlice.beginParse(cs.loadRef())))
        .signature(CryptoSignature.deserialize(cs))
        .build();
  }
}
