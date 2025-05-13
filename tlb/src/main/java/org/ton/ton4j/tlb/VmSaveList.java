package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/** _ cregs:(HashmapE 4 VmStackValue) = VmSaveList; */
@Builder
@Data
public class VmSaveList implements Serializable {
  TonHashMapE cregs;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeDict(
            cregs.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 4).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((VmStackValue) v).toCell()).endCell()))
        .endCell();
  }

  public static VmSaveList deserialize(CellSlice cs) {
    return VmSaveList.builder()
        .cregs(
            cs.loadDictE(
                4, k -> k.readUint(4), v -> VmStackValue.deserialize(CellSlice.beginParse(v))))
        .build();
  }
}
