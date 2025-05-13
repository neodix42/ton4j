package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class MultiSigV2OrderData {

  public Address multiSigAddress;
  public BigInteger orderSeqno;
  public long threshold;
  public boolean sentForExecution;
  public List<Address> signers;
  public long approvals_mask;
  public long approvals_num;
  public long expirationDate;
  public Cell order;

  @ToString.Include(name = "expirationDateString")
  public String getExpirationDateString() {
    return Utils.toUTC(expirationDate);
  }
}
