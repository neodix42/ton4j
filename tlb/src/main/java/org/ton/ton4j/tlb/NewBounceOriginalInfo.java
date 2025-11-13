package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/** _ value:CurrencyCollection created_lt:uint64 created_at:uint32 = NewBounceOriginalInfo; */
@Builder
@Data
public class NewBounceOriginalInfo implements Serializable {
  CurrencyCollection value;
  BigInteger createdLt;
  BigInteger createdAt;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeCell(value.toCell())
        .storeUint(createdLt, 64)
        .storeUint(createdAt, 32)
        .endCell();
  }

  public static NewBounceOriginalInfo deserialize(CellSlice cs) {
    return NewBounceOriginalInfo.builder()
        .value(CurrencyCollection.deserialize(cs))
        .createdLt(cs.loadUint(64))
        .createdAt(cs.loadUint(32))
        .build();
  }
}
