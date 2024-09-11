package org.ton.java.tonlib.types.globalconfig;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Builder
@Data
public class TonlibSetup implements Serializable {
    @SerializedName(value = "@type")
    String type;

    TonlibOptions options;
}
