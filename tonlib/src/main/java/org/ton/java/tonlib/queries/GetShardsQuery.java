package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.BlockIdExt;

@Builder
@Setter
@Getter
@ToString
public class GetShardsQuery {
    @SerializedName(value = "@type")
    final String type = "blocks.getShards";
    BlockIdExt id;
}
