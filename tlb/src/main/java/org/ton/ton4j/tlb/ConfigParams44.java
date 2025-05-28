package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;
import org.ton.ton4j.cell.CellSlice;
import org.ton.ton4j.cell.TonHashMapE;

/**
 *
 *
 * <pre>
 * suspended_address_list#00
 * addresses:(HashmapE 288 Unit)
 * suspended_until:uint32 = SuspendedAddressList;
 * _ SuspendedAddressList = ConfigParam 44;
 * </pre>
 */
@Builder
@Data
public class ConfigParams44 implements Serializable {
  int magic;
  TonHashMapE suspendedAddressList;
  long suspendedUntil;

  public Cell toCell() {

    return CellBuilder.beginCell()
        .storeUint(0x00, 8)
        .storeDict(
            suspendedAddressList.serialize(
                k -> CellBuilder.beginCell().storeUint((BigInteger) k, 288).endCell().getBits(),
                v -> CellBuilder.beginCell().endCell()))
        .storeUint(suspendedUntil, 32)
        .endCell();
  }

  public static ConfigParams44 deserialize(CellSlice cs) {
    return ConfigParams44.builder()
        .suspendedAddressList(cs.loadDictE(288, k -> k.readUint(288), v -> v))
        .build();
  }
}
