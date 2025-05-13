package org.ton.ton4j.liteclient.api.config;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Data
public class Validator implements Serializable {
  String publicKey;
  String adnlAddress;
  BigInteger weight;
}
