package org.ton.ton4j.tl.liteserver.responses;

import java.io.Serializable;
import lombok.Builder;

/** adnl.message.answer query_id:int256 answer:bytes = adnl.Message, id 1684ac0f */
@Builder
public class AdnlMessageAnswer implements Serializable, LiteServerAnswer {
  public static final int ADNL_MESSAGE_ANSWER = 262964246;

  byte[] answer;
  public static final int constructorId = ADNL_MESSAGE_ANSWER;

  public static AdnlMessageAnswer deserialize(byte[] payload) {
    return AdnlMessageAnswer.builder().answer(payload).build();
  }
}
