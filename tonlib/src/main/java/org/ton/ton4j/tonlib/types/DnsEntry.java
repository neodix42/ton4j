package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DnsEntry {
  @SerializedName("@type")
  final String type = "dns.entry";

  String name;
  String category;
  DnsEntryData entry;
}
