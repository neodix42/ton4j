package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.types.AccountAddressOnly;

@SuperBuilder
@Setter
@Getter
@ToString
public class LoadContractQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "smc.load";
    AccountAddressOnly account_address;
}