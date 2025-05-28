package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.AccountAddressOnly;

@Builder
@Data
public class GetAccountStateQueryOnly extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "getAccountState";

  AccountAddressOnly account_address;
}
