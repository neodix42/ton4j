package org.ton.ton4j.tl.types;

import java.io.Serializable;
import lombok.Builder;
import org.ton.ton4j.utils.Utils;

/** adnl.message.part hash:int256 total_size:int offset:int data:bytes = adnl.Message */
@Builder
public class AdnlMessagePart implements Serializable, LiteServerAnswer {
  public static final int ADNL_MESSAGE_PART = -45798087;

  byte[] answer;
  public static final int constructorId =
      (int)
          Utils.getQueryCrc32IEEEE(
              "adnl.message.part hash:int256 total_size:int offset:int data:bytes = adnl.Message;");

  public static AdnlMessagePart deserialize(byte[] payload) {
    return AdnlMessagePart.builder().answer(payload).build();
  }
}
