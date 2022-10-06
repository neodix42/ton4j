package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Builder
@Setter
@Getter
@ToString
public class RunMethodIntQuery {
    @SerializedName(value = "@type")
    final String type = "smc.runGetMethod";
    long id;
    MethodNumber method;
    String stack;
}


