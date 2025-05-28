package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AccountTransactionId implements Serializable {

  @SerializedName("@type")
  final String type = "blocks.accountTransactionId";

  String account; // after_hash
  long lt; // after_lt
}
