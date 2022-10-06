package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.BlockId;

@Builder
@Setter
@Getter
@ToString
public class LookupBlockQuery {
    @SerializedName(value = "@type")
    final String type = "blocks.lookupBlock";
    long mode;
    BlockId id;
    long lt;
    long utime;
}
