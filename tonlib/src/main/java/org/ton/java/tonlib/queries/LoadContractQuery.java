package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.java.tonlib.types.AccountAddressOnly;

@Builder
@Data
public class LoadContractQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "smc.load";

  AccountAddressOnly account_address;
}
