package org.ton.ton4j.toncenterv3.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.Map;

/**
 * Address book mapping addresses to user-friendly information
 */
@Data
public class AddressBook {
    private Map<String, AddressBookEntry> addresses;
    
    @Data
    public static class AddressBookEntry {
        @SerializedName("user_friendly")
        private String userFriendly;
        
        @SerializedName("domain")
        private String domain;
    }
}
