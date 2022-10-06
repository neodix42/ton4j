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
public class GetAccountStateQueryOnly {
    @SerializedName(value = "@type")
    final String type = "getAccountState";
    AccountAddressOnly account_address;
}