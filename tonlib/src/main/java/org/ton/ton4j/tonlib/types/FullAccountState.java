package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class FullAccountState implements Serializable {
  @SerializedName(value = "@type")
  final String type = "FullAccountState"; // response to fullAccountState

  AccountAddressOnly address;
  String balance;
  List<ExtraCurrency> extra_currencies;
  LastTransactionId last_transaction_id;
  BlockIdExt block_id;
  long sync_utime;
  AccountState account_state;
  long revision;
}
