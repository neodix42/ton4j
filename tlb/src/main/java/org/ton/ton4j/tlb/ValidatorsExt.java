package org.ton.ton4j.tlb;

import static java.util.Objects.isNull;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>{@code
 * validators_ext#12
 *   utime_since:uint32
 *   utime_until:uint32
 *   total:(## 16)
 *   main:(## 16) { main <= total } { main >= 1 }
 *   total_weight:uint64
 *   list:(HashmapE 16 ValidatorDescr) = ValidatorSet;
 *
 * }</pre>
 */
@Builder
@Data
public class ValidatorsExt implements ValidatorSet {
  long magic;
  long uTimeSince;
  long uTimeUntil;
  int total;
  int main;
  BigInteger totalWeight;
  TonHashMapE list;

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x12, 8)
        .storeUint(uTimeSince, 32)
        .storeUint(uTimeUntil, 32)
        .storeUint(total, 16)
        .storeUint(main, 16)
        .storeUint(totalWeight, 64)
        .storeDict(
            list.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 16).endCell().getBits(),
                v -> CellBuilder.beginCell().storeCell(((ValidatorDescr) v).toCell()).endCell()))
        .endCell();
  }

  public static Validators deserialize(CellSlice cs) {
    return Validators.builder()
        .magic(cs.loadUint(8).intValue())
        .uTimeSince(cs.loadUint(32).longValue())
        .uTimeUntil(cs.loadUint(32).longValue())
        .total(cs.loadUint(16).intValue())
        .main(cs.loadUint(16).intValue())
        .total(cs.loadUint(64).intValue())
        .list(
            cs.loadDictE(
                16, k -> k.readUint(16), v -> ValidatorDescr.deserialize(CellSlice.beginParse(v))))
        .build();
  }

  public List<ValidatorDescr> getValidatorsAsList() {
    List<ValidatorDescr> validatorDescrs = new ArrayList<>();
    if (isNull(list)) {
      return validatorDescrs;
    }
    for (Map.Entry<Object, Object> entry : list.elements.entrySet()) {
      validatorDescrs.add((ValidatorDescr) entry.getValue());
    }
    return validatorDescrs;
  }
}
