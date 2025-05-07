package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UpdateSyncStateQuery extends ExtraQuery {
  @SerializedName(value = "@type")
  final String type = "updateSyncState";

  long id; // result from createQuery
}
