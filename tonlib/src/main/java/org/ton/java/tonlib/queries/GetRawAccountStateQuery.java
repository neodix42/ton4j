package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.java.tonlib.types.AccountAddress;

@Builder
@Data
public class GetRawAccountStateQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "raw.getAccountState";

  AccountAddress account_address;
}
