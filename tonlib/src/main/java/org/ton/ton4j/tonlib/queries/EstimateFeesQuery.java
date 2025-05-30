package org.ton.ton4j.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
public class EstimateFeesQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "query.estimateFees";
    @SerializedName(value = "id")
    long queryId;
    boolean ignore_chksig;
}
