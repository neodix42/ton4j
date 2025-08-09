package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Request model for runGetMethod endpoint
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunGetMethodRequest {
    
    @SerializedName("address")
    private String address;
    
    @SerializedName("method")
    private Object method; // Can be String or Integer
    
    @SerializedName("stack")
    private List<List<Object>> stack;
    
    @SerializedName("seqno")
    private Long seqno;
    
    public RunGetMethodRequest(String address, Object method, List<List<Object>> stack) {
        this.address = address;
        this.method = method;
        this.stack = stack;
    }
}
