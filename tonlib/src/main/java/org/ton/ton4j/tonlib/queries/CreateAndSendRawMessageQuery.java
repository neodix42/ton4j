package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.tonlib.types.AccountAddressOnly;

@Builder
@Data
public class CreateAndSendRawMessageQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "raw.createAndSendMessage";

  AccountAddressOnly destination;
  String initial_account_state;
  String data;
}
