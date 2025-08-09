package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class DetectAddressResponse {
  @SerializedName("raw_form")
  private String rawForm;

  @SerializedName("bounceable")
  private AddressForm bounceable;

  @SerializedName("non_bounceable")
  private AddressForm nonBounceable;

  @SerializedName("given_type")
  private String givenType;

  @SerializedName("test_only")
  private Boolean testOnly;

  @Data
  public static class AddressForm {
    @SerializedName("b64")
    private String b64;

    @SerializedName("b64url")
    private String b64url;
  }
}
