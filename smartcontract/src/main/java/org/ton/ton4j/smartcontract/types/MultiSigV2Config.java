package org.ton.ton4j.smartcontract.types;

import static org.ton.ton4j.smartcontract.multisig.MultiSigWalletV2.toProposersDict;
import static org.ton.ton4j.smartcontract.multisig.MultiSigWalletV2.toSignersDict;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.cell.CellBuilder;

@Builder
@Data
public class MultiSigV2Config {

  public BigInteger nextOrderSeqno;
  public List<Address> signers;
  public List<Address> proposers;
  public long numberOfSigners;
  public long threshold;
  public boolean allowArbitraryOrderSeqno;

  public Cell toCell() {
    return CellBuilder.beginCell()
        .storeUint(nextOrderSeqno, 256)
        .storeUint(threshold, 8)
        .storeRef(toSignersDict(signers))
        .storeUint(numberOfSigners, 8)
        .storeDict(toProposersDict(proposers))
        .storeBit(allowArbitraryOrderSeqno)
        .endCell();
  }
}
