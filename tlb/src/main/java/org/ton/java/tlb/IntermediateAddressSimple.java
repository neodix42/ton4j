package org.ton.java.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

/**
 *
 *
 * <pre>
 * interm_addr_simple$10 workchain_id:int8 addr_pfx:uint64 = IntermediateAddress;
 * </pre>
 */
@Builder
@Data
public class IntermediateAddressSimple implements IntermediateAddress, Serializable {
  int workchainId;
  BigInteger addrPfx;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeBit(true)
        .storeBit(false)
        .storeUint(workchainId, 8)
        .storeUint(addrPfx, 64)
        .endCell();
  }
}
