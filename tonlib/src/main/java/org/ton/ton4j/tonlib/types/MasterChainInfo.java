package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class MasterChainInfo implements Serializable {

  @SerializedName("@type")
  final String type = "blocks.masterchainInfo";

  BlockIdExt last;
  String state_root_hash;
  BlockIdExt init;
}
