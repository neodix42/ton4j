package org.ton.ton4j.smartcontract.types;

import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.address.Address;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class ChannelData {
  long state;
  BigInteger balanceA;
  BigInteger balanceB;
  public byte[] publicKeyA;
  public byte[] publicKeyB;
  BigInteger channelId;
  long quarantineDuration;
  BigInteger misbehaviorFine;
  long conditionalCloseDuration;
  BigInteger seqnoA;
  BigInteger seqnoB;
  Cell quarantine;
  BigInteger excessFee;
  Address addressA;
  Address addressB;

  public String getPublicKeyA() {
    return Utils.bytesToHex(publicKeyA);
  }

  public String getPublicKeyB() {
    return Utils.bytesToHex(publicKeyA);
  }
}
