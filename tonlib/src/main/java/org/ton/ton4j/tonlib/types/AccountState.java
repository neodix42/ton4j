package org.ton.ton4j.tonlib.types;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AccountState implements Serializable {
  String code;
  String data;
  String frozen_hash;
  long wallet_id;
  int seqno;
}
