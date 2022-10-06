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
public class BlockId {
    @SerializedName(value = "@type")
    final String type = "ton.blockId";
    long workchain;
    long shard;
    long seqno;
}
