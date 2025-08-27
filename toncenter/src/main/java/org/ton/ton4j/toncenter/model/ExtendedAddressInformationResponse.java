package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.math.BigInteger;
import org.ton.ton4j.utils.Utils;

@Data
public class ExtendedAddressInformationResponse {
  @SerializedName("@type")
  private String type;

  private String balance;

  @SerializedName("account_state")
  private Object accountState;

  @SerializedName("address")
  private AddressInfo address;

  @SerializedName("extra_currencies")
  private Object[] extraCurrencies;

  private Integer revision;

  @SerializedName("@extra")
  private String extra;

  @SerializedName("last_transaction_id")
  private TransactionId lastTransactionId;

  @SerializedName("block_id")
  private BlockId blockId;

  @SerializedName("sync_utime")
  private Long syncUtime;

  @Data
  public static class AddressInfo {
    @SerializedName("@type")
    private String type;

    @SerializedName("account_address")
    private String accountAddress;
  }

  @Data
  public static class TransactionId {
    @SerializedName("@type")
    private String type;

    private String lt;
    private String hash;
  }

  @Data
  public static class BlockId {
    @SerializedName("@type")
    private String type;

    private Long workchain;
    private BigInteger shard;
    private Long seqno;

    @SerializedName("root_hash")
    private String rootHash;

    @SerializedName("file_hash")
    private String fileHash;
    
    @Override
    public String toString() {
      return "BlockId{" +
          "type='" + type + '\'' +
          ", workchain=" + workchain +
          ", shard=0x" + Utils.bigIntegerToUnsignedHex(shard) +
          ", seqno=" + seqno +
          ", rootHash='" + rootHash + '\'' +
          ", fileHash='" + fileHash + '\'' +
          '}';
    }
  }
}
