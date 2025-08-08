package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * Response for getTokenData endpoint
 */
@Data
public class TokenDataResponse {
    
    @SerializedName("next_item_index")
    private Long nextItemIndex;
    
    @SerializedName("collection_content")
    private CollectionContent collectionContent;
    
    @SerializedName("contract_type")
    private String contractType;
    
    @SerializedName("jetton_content")
    private JettonContent jettonContent;
    
    @SerializedName("jetton_wallet")
    private JettonWallet jettonWallet;
    
    @SerializedName("nft_content")
    private NftContent nftContent;
    
    @SerializedName("nft_item")
    private NftItem nftItem;
    
    @Data
    public static class CollectionContent {
        private String type;
        private String data;
    }
    
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
    public static class JettonWallet {
        private String balance;
        private String owner;
        private String jetton;
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
    
    @Data
    public static class NftItem {
        private String owner;
        private String collection;
        private Boolean verified;
        private NftContent content;
    }
}
