package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;

@Builder
@Data
public class MultiSigV2Data {

  public BigInteger nextOrderSeqno;
  public long threshold;
  public List<Address> signers;
  public List<Address> proposers;
}
