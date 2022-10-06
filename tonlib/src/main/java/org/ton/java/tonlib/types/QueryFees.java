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
public class QueryFees {
    @SerializedName("@type")
    final String type = "query.fees";
    Fees source_fees;
    List<Fees> destination_fees;
}

