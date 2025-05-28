package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.AccountAddressOnly;

@Builder
@Data
public class GetRawAccountStateQueryOnly extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "raw.getAccountState";

  AccountAddressOnly account_address;
}
