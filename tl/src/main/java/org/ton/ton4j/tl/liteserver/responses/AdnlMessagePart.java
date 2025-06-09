package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import lombok.Builder;

/** adnl.message.part hash:int256 total_size:int offset:int data:bytes = adnl.Message */
@Builder
public class AdnlMessagePart implements Serializable, LiteServerAnswer {
  public static final int ADNL_MESSAGE_PART = 1022479244;

  byte[] answer;
  public static final int constructorId = ADNL_MESSAGE_PART;

  public static AdnlMessagePart deserialize(byte[] payload) {
    return AdnlMessagePart.builder().answer(payload).build();
  }
}
