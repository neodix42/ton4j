package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class DnsResolved {
    @SerializedName("@type")
    final String type = "dns.resolved";
    List<DnsEntry> entries;
}
