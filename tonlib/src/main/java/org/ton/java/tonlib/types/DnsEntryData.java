package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class DnsEntryData {
  @SerializedName("@type")
  String type;

  String bytes;
  String text;
  AccountAddressOnly resolver;
  AccountAddressOnly smc_address;
  AccountAddressOnly adnl_address;
}
