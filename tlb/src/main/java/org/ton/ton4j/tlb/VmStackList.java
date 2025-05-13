package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 * vm_stk_nil#_ = VmStackList 0; vm_stk_cons#_ {n:#} rest:^(VmStackList n) tos:VmStackValue =
 * VmStackList (n + 1);
 */
@Builder
@Data
public class VmStackList implements Serializable {
  List<VmStackValue> tos;

  /**
   * Also knows as serializeTuple() in ton-core web.
   *
   * @return Cell
   */
  public Cell toCell() {
    Cell list = CellBuilder.beginCell().endCell();
    for (VmStackValue value : tos) {
      Cell valueCell = value.toCell();
      list =
          CellBuilder.beginCell()
              .storeRef(list) // rest
              .storeCell(valueCell) // tos
              .endCell();
    }
    return list;
  }

  /**
   * Also known as parseTuple() in ton-core web.
   *
   * @param cs CellSlice
   * @return VmStackList
   */
  public static VmStackList deserialize(CellSlice cs, int depth) {

    if (depth == 0) {
      return VmStackList.builder().tos(Collections.emptyList()).build();
    }

    List<VmStackValue> ar1 =
        new ArrayList<>(deserialize(CellSlice.beginParse(cs.loadRef()), depth - 1).getTos());
    ar1.add(VmStackValue.deserialize(cs));
    return VmStackList.builder().tos(ar1).build();
  }
}
