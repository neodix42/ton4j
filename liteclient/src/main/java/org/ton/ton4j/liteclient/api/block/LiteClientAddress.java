package org.ton.ton4j.liteclient.api.block;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

import static java.util.Objects.nonNull;

@Builder
@ToString
@Getter
public class LiteClientAddress implements Serializable {
  Long wc;
  String addr;

  public String getAddress() {
    if (nonNull(wc)) {
      return wc + ":" + addr;
    } else {
      return "";
    }
  }
}
