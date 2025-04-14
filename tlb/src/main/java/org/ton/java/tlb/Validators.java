package org.ton.java.tlb;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;
import org.ton.java.cell.CellSlice;
import org.ton.java.cell.TonHashMap;

/**
 *
 *
 * <pre>{@code
 * validators#11
 *   utime_since:uint32
 *   utime_until:uint32
 *   total:(## 16)
 *   main:(## 16) { main <= total } { main >= 1 }
 *   list:(Hashmap 16 ValidatorDescr) = ValidatorSet;
 *
 * }</pre>
 */
@Builder
@Data
public class Validators implements ValidatorSet {
  int magic;
  long uTimeSince;
  long uTimeUntil;
  int total;
  int main;
  TonHashMap list;

  private String getMagic() {
    return Long.toBinaryString(magic);
  }

  @Override
  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x11, 8)
        .storeUint(uTimeSince, 32)
        .storeUint(uTimeUntil, 32)
        .storeUint(total, 16)
        .storeUint(main, 16)
        .storeDict(
            list.serialize(
                k -> CellBuilder.beginCell().storeUint((Long) k, 16).endCell().getBits(),
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
        .list(
            cs.loadDict(
                16, k -> k.readUint(16), v -> ValidatorDescr.deserialize(CellSlice.beginParse(v))))
        .build();
  }

  public List<Validator> getValidatorsAsList() {
    List<Validator> validators = new ArrayList<>();
    if (isNull(list)) {
      return validators;
    }
    for (Map.Entry<Object, Object> entry : list.elements.entrySet()) {
      validators.add((Validator) entry.getValue());
    }
    return validators;
  }

  public List<ValidatorAddr> getValidatorsAddrAsList() {
    List<ValidatorAddr> validatorAddrs = new ArrayList<>();
    if (isNull(list)) {
      return validatorAddrs;
    }
    for (Map.Entry<Object, Object> entry : list.elements.entrySet()) {
      validatorAddrs.add((ValidatorAddr) entry.getValue());
    }
    return validatorAddrs;
  }
}
