package org.ton.ton4j.tl.types;

import lombok.Builder;
import org.ton.ton4j.utils.Utils;

/** adnl.message.answer query_id:int256 answer:bytes = adnl.Message, id 1684ac0f */
@Builder
public class AdnlMessageAnswer {
  byte[] answer;
  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "adnl.message.answer query_id:int256 answer:bytes = adnl.Message");

  public static AdnlMessageAnswer deserialize(byte[] payload) {
    return AdnlMessageAnswer.builder().answer(payload).build();
  }
}
