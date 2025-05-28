package org.ton.ton4j.tlb;

import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

/**
 *
 *
 * <pre>
 * interm_addr_ext$11 workchain_id:int32 addr_pfx:uint64 = IntermediateAddress;
 * </pre>
 */
@Builder
@Data
public class IntermediateAddressExt implements IntermediateAddress, Serializable {
  long workchainId;
  BigInteger addrPfx;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeBit(true)
        .storeBit(true)
        .storeUint(workchainId, 32)
        .storeUint(addrPfx, 64)
        .endCell();
  }
}
