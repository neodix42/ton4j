package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

@Data
public class EstimateFeeRequest {
    private String address;
    private String body;
    
    @SerializedName("init_code")
    private String initCode = "";
    
    @SerializedName("init_data")
    private String initData = "";
    
    @SerializedName("ignore_chksig")
    private Boolean ignoreChksig = true;
}
