package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.BlockIdExt;
import org.ton.java.tonlib.types.AccountTransactionId;

@Builder
@Setter
@Getter
@ToString
public class GetBlockTransactionsQuery {
    @SerializedName(value = "@type")
    final String type = "blocks.getTransactions";
    BlockIdExt id;
    int mode;
    long count;
    AccountTransactionId after;
}
