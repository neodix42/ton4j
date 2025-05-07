package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DnsResolved {
  @SerializedName("@type")
  final String type = "dns.resolved";

  List<DnsEntry> entries;
}
