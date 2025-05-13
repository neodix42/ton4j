package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.utils.Utils;

@Builder
@Data
public class BlockIdExt implements Serializable {

  @SerializedName("@type")
  final String type = "ton.blockIdExt";

  long workchain;
  long shard;
  long seqno;
  String root_hash;
  String file_hash;

  public String getShortBlockSeqno() {
    return String.format(
        "(%d,%s,%d)", workchain, Utils.longToUnsignedBigInteger(shard).toString(16), seqno);
  }

  public String getFullBlockSeqno() {
    return String.format(
        "(%d,%s,%d):%s:%s",
        workchain, Utils.longToUnsignedBigInteger(shard).toString(16), seqno, root_hash, file_hash);
  }
}
