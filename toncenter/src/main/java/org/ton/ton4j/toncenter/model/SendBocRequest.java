package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request model for sendBoc and sendBocReturnHash endpoints */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SendBocRequest {

  @SerializedName("boc")
  private String boc;
}
