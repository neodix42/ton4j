package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response for getTokenData endpoint
 */
@Data
public class TokenDataResponse {
    
    @SerializedName("jetton_content")
    private JettonContent jettonContent;
    
    @SerializedName("jetton_wallet")
    private Boolean jettonWallet;
    
    @SerializedName("nft_content")
    private NftContent nftContent;
    
    @SerializedName("nft_item")
    private Boolean nftItem;
    
    @SerializedName("nft_collection")
    private Boolean nftCollection;
    
    @Data
    public static class JettonContent {
        private String type;
        private String uri;
        private String name;
        private String description;
        private String image;
        private String symbol;
        private String decimals;
    }
    
    @Data
    public static class NftContent {
        private String type;
        private String uri;
        private String name;
        private String description;
        private String image;
        
        @SerializedName("image_data")
        private String imageData;
    }
}
