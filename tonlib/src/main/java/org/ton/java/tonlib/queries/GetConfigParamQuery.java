package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.BlockIdExt;

@Builder
@Setter
@Getter
@ToString
public class GetConfigParamQuery {
    @SerializedName("@type")
    final String type = "getConfigParam";
    BlockIdExt id;
    long param;
    long mode;
}
