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
public class DnsResolveQuery {
    @SerializedName(value = "@type")
    final String type = "dns.resolve";
    AccountAddressOnly account_address;
    String name;
    String category;
    int ttl;
}
