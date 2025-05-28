package org.ton.ton4j.tlb;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

/**
 *
 *
 * <pre>{@code
 * interm_addr_regular$0 use_dest_bits:(#<= 96) = IntermediateAddress;
 * }</pre>
 */
@Builder
@Data
public class IntermediateAddressRegular implements IntermediateAddress, Serializable {
  int use_dest_bits; // 7 bits

  public Cell toCell() {
    return CellBuilder.beginCell().storeBit(false).storeUint(use_dest_bits, 7).endCell();
  }
}
