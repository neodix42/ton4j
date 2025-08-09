package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.List;

/** Response for estimateFee endpoint */
@Data
public class EstimateFeeResponse {

  @SerializedName("@type")
  private String type;

  @SerializedName("source_fees")
  private Fees sourceFees;

  @SerializedName("destination_fees")
  private List<Fees> destinationFees;

  @SerializedName("@extra")
  private String extra;

  @Data
  public static class Fees {
    @SerializedName("@type")
    private String type;

    @SerializedName("in_fwd_fee")
    private Long inFwdFee;

    @SerializedName("storage_fee")
    private Long storageFee;

    @SerializedName("gas_fee")
    private Long gasFee;

    @SerializedName("fwd_fee")
    private Long fwdFee;
  }
}
