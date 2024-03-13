package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.AccountAddressOnly;

@Builder
@Setter
@Getter
@ToString
public class CreateAndSendRawMessageQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "raw.createAndSendMessage";
    AccountAddressOnly destination;
    String initial_account_state;
    String data;
}
