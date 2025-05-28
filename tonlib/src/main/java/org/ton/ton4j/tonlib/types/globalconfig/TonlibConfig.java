package org.ton.ton4j.tonlib.types.globalconfig;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Builder
@Setter
@Getter
public class TonlibConfig {
    @SerializedName(value = "@type")
    String type;

    String config;
    boolean use_callbacks_for_network;
    String blockchain_name;
    boolean ignore_cache;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
    }
}