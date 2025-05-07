package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.math.BigInteger;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class LastTransactionId implements Serializable {
  @SerializedName("@type")
  final String type = "internal.transactionId"; // not necessary

  BigInteger lt;
  String hash;
}
