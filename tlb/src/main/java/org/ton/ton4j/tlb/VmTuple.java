package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>
 * vm_tuple_nil$_ = VmTuple 0;
 * vm_tuple_tcons$_ {n:#} head:(VmTupleRef n) tail:^VmStackValue = VmTuple (n + 1);
 * </pre>
 */
@Builder
@Data
public class VmTuple implements VmStackValue, Serializable {
  List<VmStackValue> values;

  public Cell toCell() {
    List<VmStackValue> pValues = new ArrayList<>(values);
    if (pValues.isEmpty()) {
      return CellBuilder.beginCell().endCell();
    }

    VmStackValue v = pValues.get(pValues.size() - 1);
    pValues.remove(pValues.size() - 1);
    return CellBuilder.beginCell()
        .storeCell(VmTupleRef.toCellS(pValues))
        .storeRef(v.toCell())
        .endCell();
  }

  public static Cell toCell(List<VmTuple> pValues) {
    List<VmTuple> lValues = new ArrayList<>(pValues);

    if (lValues.isEmpty()) {
      return CellBuilder.beginCell().endCell();
    }

    VmTuple v = lValues.get(lValues.size() - 1);
    lValues.remove(lValues.size() - 1);
    return CellBuilder.beginCell()
        .storeCell(VmTupleRef.toCell(lValues))
        .storeRef(((VmStackValue) v).toCell())
        .endCell();
  }

  public static Cell toCellS(List<VmStackValue> pValues) {
    List<VmStackValue> lValues = new ArrayList<>(pValues);

    if (lValues.isEmpty()) {
      return CellBuilder.beginCell().endCell();
    }

    VmStackValue v = lValues.get(lValues.size() - 1);
    lValues.remove(lValues.size() - 1);
    return CellBuilder.beginCell()
        .storeCell(VmTupleRef.toCellS(lValues))
        .storeRef(v.toCell())
        .endCell();
  }

  public static VmTuple deserialize(CellSlice cs, int len) {
    if (len == 0) {
      return VmTuple.builder().values(Collections.emptyList()).build();
    }

    ArrayList<VmStackValue> ar = new ArrayList<>(VmTupleRef.deserialize(cs, len - 1).getValues());
    ar.add(
        cs.getRefsCount() > 0
            ? VmStackValue.deserialize(CellSlice.beginParse(cs.loadRef()))
            : null);

    return VmTuple.builder().values(ar).build();
  }

  public BigInteger getIntByIndex(int stackIndex) {

    VmStackValue vmStackValue = values.get(stackIndex);
    if (vmStackValue instanceof VmStackValueInt) {
      return ((VmStackValueInt) vmStackValue).getValue();
    } else if (vmStackValue instanceof VmStackValueTinyInt) {
      return ((VmStackValueTinyInt) vmStackValue).getValue();
    } else {
      throw new RuntimeException(
          "Unsupported vm tuple value type: " + vmStackValue + ". Expecting number.");
    }
  }

  public Cell getCellByIndex(int stackIndex) {
    if (values.get(stackIndex) instanceof VmStackValueCell) {
      return ((VmStackValueCell) values.get(stackIndex)).getCell();
    } else if (values.get(stackIndex) instanceof VmStackValueSlice) {
      return ((VmStackValueSlice) values.get(stackIndex)).getCell().getCell();
    } else {
      throw new RuntimeException("Unsupported vm tuple value type: " + values.get(stackIndex));
    }
  }
}
