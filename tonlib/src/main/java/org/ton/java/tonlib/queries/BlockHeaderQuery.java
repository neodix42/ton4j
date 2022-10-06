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
public class BlockHeaderQuery {
    @SerializedName(value = "@type")
    final String type = "blocks.getBlockHeader";
    BlockIdExt id;
}
