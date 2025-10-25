package org.ton.ton4j.toncenterv3.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Error response from TonCenter V3 API
 */
@Data
public class RequestError {
    @SerializedName("code")
    private Integer code;
    
    @SerializedName("error")
    private String error;
}
