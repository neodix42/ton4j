package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response for sendBoc endpoint
 */
@Data
public class SendBocResponse {
    
    @SerializedName("code")
    private Integer code;
    
    @SerializedName("hash")
    private String hash;
}
