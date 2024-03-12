package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.ton.java.tonlib.types.BlockIdExt;

@SuperBuilder
@Setter
@Getter
@ToString
public class WithBlockQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "withBlock";
    BlockIdExt id;
    LoadContractQuery function;
}