package org.ton.java.smartcontract.types;

import static java.util.Objects.nonNull;

import java.math.BigInteger;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import org.ton.java.address.Address;
import org.ton.java.cell.Cell;
import org.ton.java.utils.Utils;

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
  @ToString.Exclude public Cell order;

  @ToString.Include(name = "orderBoc")
  public String getOrderBoc() {
    return nonNull(order) ? order.toHex() : "";
  }

  @ToString.Include(name = "expirationDateString")
  public String getExpirationDateString() {
    return Utils.toUTC(expirationDate);
  }
}
