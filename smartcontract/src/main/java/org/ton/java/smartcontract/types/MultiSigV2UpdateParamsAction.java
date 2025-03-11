package org.ton.java.smartcontract.types;

import static org.ton.java.smartcontract.multisig.MultiSigWalletV2.toProposersDict;
import static org.ton.java.smartcontract.multisig.MultiSigWalletV2.toSignersDict;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellBuilder;

@Builder
@Data
public class MultiSigV2UpdateParamsAction implements MultiSigV2Action {
  long newThreshold;
  List<Address> newSigners;
  List<Address> newProposers;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(0x1d0cfbd3L, 32)
        .storeUint(newThreshold, 8)
        .storeRef(toSignersDict(newSigners))
        .storeDict(toProposersDict(newProposers))
        .endCell();
  }
}
