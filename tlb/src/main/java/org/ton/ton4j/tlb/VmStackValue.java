package org.ton.ton4j.tlb;

import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellSlice;

/**
 *
 *
 * <pre>{@code
 * vm_stk_null#00 = VmStackValue;
 * vm_stk_tinyint#01 value:int64 = VmStackValue;
 * vm_stk_int#0201_ value:int257 = VmStackValue;
 * vm_stk_nan#02ff = VmStackValue;
 * vm_stk_cell#03 cell:^Cell = VmStackValue;
 * cell:^Cell st_bits:(## 10) end_bits:(## 10) { st_bits <= end_bits } st_ref:(#<= 4) end_ref:(#<= 4) { st_ref <= end_ref } = VmCellSlice;
 * vm_stk_slice#04 _:VmCellSlice = VmStackValue;
 * vm_stk_builder#05 cell:^Cell = VmStackValue;
 * vm_stk_cont#06 cont:VmCont = VmStackValue;
 * vm_tupref_nil$_ = VmTupleRef 0;
 * vm_tupref_single$_ entry:^VmStackValue = VmTupleRef 1;
 * vm_tupref_any$_ {n:#} ref:^(VmTuple (n + 2)) = VmTupleRef (n + 2);
 * vm_tuple_nil$_ = VmTuple 0;
 * vm_tuple_tcons$_ {n:#} head:(VmTupleRef n) tail:^VmStackValue = VmTuple (n + 1);
 * vm_stk_tuple#07 len:(## 16) data:(VmTuple len) = VmStackValue;
 * }</pre>
 */
public interface VmStackValue {

  Cell toCell();

  static VmStackValue deserialize(CellSlice cs) {

    if (cs.isSliceEmpty()) {
      return null;
    }

    int tag = cs.preloadUint(15).intValue();

    if (tag == 256) {
      return VmStackValueInt.deserialize(cs);
    }

    int magic = cs.preloadUint(8).intValue();

    if (magic == 0x00) {
      return VmStackValueNull.deserialize(cs);
    } else if (magic == 0x01) {
      return VmStackValueTinyInt.deserialize(cs);
    } else if (magic == 0x02) {
      int magic2 = cs.skipBits(8).preloadUint(7).intValue();
      if (magic2 == 0) {
        return VmStackValueInt.deserialize(cs);
      } else {
        cs.loadBit();
        return VmStackValueNaN.builder().build();
      }
    } else if (magic == 0x03) {
      return VmStackValueCell.deserialize(cs);
    } else if (magic == 0x04) {
      return VmStackValueSlice.deserialize(cs);
    } else if (magic == 0x05) {
      return VmStackValueBuilder.deserialize(cs);
    } else if (magic == 0x06) {
      return VmStackValueCont.deserialize(cs);
    } else if (magic == 0x07) {
      int len = cs.skipBits(8).loadUint(16).intValue();
      return VmTuple.deserialize(cs, len);
    } else {
      throw new Error("Error deserializing VmStackValue, wrong magic " + magic);
    }
  }
}
