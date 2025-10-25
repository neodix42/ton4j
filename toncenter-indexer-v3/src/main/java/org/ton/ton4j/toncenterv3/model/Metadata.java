package org.ton.ton4j.toncenterv3.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * Metadata for addresses including token information
 */
@Data
public class Metadata {
    private Map<String, AddressMetadata> addresses;
    
    @Data
    public static class AddressMetadata {
        @SerializedName("is_indexed")
        private Boolean isIndexed;
        
        @SerializedName("token_info")
        private List<TokenInfo> tokenInfo;
    }
    
    @Data
    public static class TokenInfo {
        @SerializedName("type")
        private String type;
        
        @SerializedName("name")
        private String name;
        
        @SerializedName("symbol")
        private String symbol;
        
        @SerializedName("description")
        private String description;
        
        @SerializedName("image")
        private String image;
        
        @SerializedName("nft_index")
        private String nftIndex;
        
        @SerializedName("valid")
        private Boolean valid;
        
        @SerializedName("extra")
        private Map<String, Object> extra;
    }
}
