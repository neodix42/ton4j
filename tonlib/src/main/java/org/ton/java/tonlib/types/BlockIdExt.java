package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.utils.Utils;

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

    public String getShortBlockSeqno() {
        return String.format("(%d,%s,%d)", workchain, Utils.longToUnsignedBigInteger(shard).toString(16), seqno);
    }

    public String getFullBlockSeqno() {
        return String.format("(%d,%s,%d):%s:%s", workchain, Utils.longToUnsignedBigInteger(shard).toString(16), seqno, root_hash, file_hash);
    }
}

