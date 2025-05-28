package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigInteger;

@Builder
@Data
public class TvmStackEntryNumber extends TvmStackEntry implements Serializable {
  @SerializedName("@type")
  final String type = "tvm.stackEntryNumber";

  TvmNumber number;

  public BigInteger getNumber() {
    return new BigInteger(number.getNumber());
  }
}
