package org.ton.ton4j.tonlib.types.globalconfig;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class TonGlobalConfig implements Serializable {
    @SerializedName(value = "@type")
    String type;

    LiteServers[] liteservers;
    Validator validator;
    Dht dht;
}
