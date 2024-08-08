package org.ton.java.tonlib.types.globalconfig;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Getter
@Setter
@ToString
public class TonGlobalConfig implements Serializable {
    @SerializedName(value = "@type")
    String type;

    LiteServers[] liteservers;
    Validator validator;
    Dht dht;
}
