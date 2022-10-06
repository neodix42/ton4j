package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class UpdateSyncState {
    @SerializedName(value = "@type")
    final String type = "updateSyncState";
    SyncStateInProgress sync_state;
}

