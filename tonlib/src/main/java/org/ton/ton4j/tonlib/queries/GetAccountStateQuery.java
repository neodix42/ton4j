package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ton.ton4j.tonlib.types.AccountAddress;

@EqualsAndHashCode(callSuper = true)
@Builder
@Data
public class GetAccountStateQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "getAccountState";

  AccountAddress account_address;
}
