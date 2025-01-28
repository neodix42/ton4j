package org.ton.java.tonlib.types;

import static java.util.Objects.nonNull;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.codec.binary.Hex;
import org.ton.java.cell.Cell;
import org.ton.java.cell.CellSlice;
import org.ton.java.utils.Utils;

@Builder
@Data
public class RawMessage implements Serializable {
  @SerializedName("@type")
  final String type = "raw.message";

  String hash;
  AccountAddressOnly source;
  AccountAddressOnly destination;
  String value;
  List<ExtraCurrency> extra_currencies;
  String fwd_fee;
  String ihr_fee;
  long created_lt;
  String body_hash;
  MsgData msg_data;

  /**
   * Returns base64 result which is encoded in base64 by default. This is a wrapper around
   * msg_data.getBody(), but additionally decodes text message from base64 to plain string. Also see
   * <b>getComment()</b> to load long text messages;
   *
   * @return String
   */
  public String getMessage() {
    if (nonNull(msg_data.getBody())) {
      return msg_data.getBody();
    } else {
      return Utils.base64ToString(msg_data.getText());
    }
  }

  /**
   * Returns decoded base64 result converted to bytes
   *
   * @return byte[]
   */
  public byte[] getMessageBytes() {
    if (nonNull(msg_data.getBody())) {
      return Utils.base64ToString(msg_data.getBody()).getBytes();
    } else {
      return Utils.base64ToString(msg_data.getText()).getBytes();
    }
  }

  /**
   * Returns decoded base64 result converted to hex
   *
   * @return byte[]
   */
  public String getMessageHex() {
    if (nonNull(msg_data.getBody())) {
      return Hex.encodeHexString(Utils.base64ToString(msg_data.getBody()).getBytes());
    } else {
      return Hex.encodeHexString(Utils.base64ToString(msg_data.getText()).getBytes());
    }
  }

  public String getComment() {
    if (nonNull(msg_data.getText())) {
      return CellSlice.beginParse(Cell.fromHex(Utils.base64ToHexString(msg_data.getText())))
          .loadSnakeString();
    } else {
      return "";
    }
  }
}
