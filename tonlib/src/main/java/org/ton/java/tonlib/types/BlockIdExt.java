package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class BlockIdExt implements Serializable {
    @SerializedName("@type")
    final String type = "ton.blockIdExt";
    long workchain;
    long shard;
    long seqno;
    String root_hash;
    String file_hash;
}

