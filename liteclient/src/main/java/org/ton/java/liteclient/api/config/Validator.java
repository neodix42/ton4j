package org.ton.java.liteclient.api.config;

import lombok.Builder;import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Data
public class Validator implements Serializable {
    String publicKey;
    String adnlAddress;
    BigInteger weight;
}
