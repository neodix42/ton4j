package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class UpdateSyncState implements Serializable {
  @SerializedName(value = "@type")
  final String type = "updateSyncState";

  SyncStateInProgress sync_state;
}
