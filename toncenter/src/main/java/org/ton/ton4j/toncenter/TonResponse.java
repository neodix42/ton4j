package org.ton.ton4j.toncenter;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Generic response wrapper for all TonCenter API responses. Matches the standard TonCenter API
 * response format: {ok, result, error, code}
 *
 * @param <T> The type of the result data
 */
@Data
public class TonResponse<T> {

  /** Indicates if the request was successful */
  @SerializedName("ok")
  private boolean ok;

  /** The result data when ok=true */
  @SerializedName("result")
  private T result;

  /** Error message when ok=false */
  @SerializedName("error")
  private String error;

  /** Error code when ok=false */
  @SerializedName("code")
  private Integer code;

  /**
   * Check if the response indicates success
   *
   * @return true if the API call was successful
   */
  public boolean isSuccess() {
    return ok;
  }

  /**
   * Check if the response indicates an error
   *
   * @return true if the API call failed
   */
  public boolean isError() {
    return !ok;
  }
}
