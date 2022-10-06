package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class BlockTransactions {
    @SerializedName("@type")
    final String type = "blocks.transactions";
    BlockIdExt id;
    long req_count;
    boolean incomplete;
    List<ShortTxId> transactions;
}