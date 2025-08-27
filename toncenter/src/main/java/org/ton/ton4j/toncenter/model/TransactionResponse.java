package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/** Response model for transaction data */
@Data
public class TransactionResponse {

  @SerializedName("@type")
  private String type;

  @SerializedName("address")
  private Address address;

  @SerializedName("utime")
  private Long utime;

  @SerializedName("data")
  private String data;

  @SerializedName("transaction_id")
  private TransactionId transactionId;

  @SerializedName("fee")
  private String fee;

  @SerializedName("storage_fee")
  private String storageFee;

  @SerializedName("other_fee")
  private String otherFee;

  @SerializedName("in_msg")
  private Message inMsg;

  @SerializedName("out_msgs")
  private List<Message> outMsgs;

  @Data
  public static class Address {
    @SerializedName("@type")
    private String type;

    @SerializedName("account_address")
    private String accountAddress;
  }

  @Data
  public static class TransactionId {
    @SerializedName("@type")
    private String type;

    @SerializedName("lt")
    private String lt;

    @SerializedName("hash")
    private String hash;
  }

  @Data
  public static class Message {
    @SerializedName("@type")
    private String type;

    @SerializedName("source")
    private String source;

    @SerializedName("destination")
    private String destination;

    @SerializedName("value")
    private String value;

    @SerializedName("fwd_fee")
    private String fwdFee;

    @SerializedName("ihr_fee")
    private String ihrFee;

    @SerializedName("created_lt")
    private String createdLt;

    @SerializedName("body_hash")
    private String bodyHash;

    @SerializedName("hash")
    private String hash;

    @SerializedName("extra_currencies")
    private Object extraCurrencies;

    @SerializedName("msg_data")
    private MessageData msgData;

    @SerializedName("message")
    private String message;
  }

  @Data
  public static class MessageData {
    @SerializedName("@type")
    private String type;

    @SerializedName("body")
    private String body;

    @SerializedName("init_state")
    private String initState;
  }
}
