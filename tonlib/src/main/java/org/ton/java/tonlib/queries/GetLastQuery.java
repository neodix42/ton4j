package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class GetLastQuery {
    @SerializedName("@type")
    final String type = "blocks.getMasterchainInfo";
}
