package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class RawAccountState implements Serializable {
  @SerializedName(value = "@type")
  final String type = "raw.fullAccountState"; // response to raw.getAccountState

  String balance;
  List<ExtraCurrency> extra_currencies;
  String code;
  String data;
  LastTransactionId last_transaction_id;
  BlockIdExt block_id;
  String frozen_hash;
  long sync_utime;
}
