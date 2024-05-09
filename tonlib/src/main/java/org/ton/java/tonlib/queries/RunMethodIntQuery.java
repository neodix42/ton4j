package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.TvmStackEntry;

import java.util.Deque;

@Builder
@Setter
@Getter
@ToString
public class RunMethodIntQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "smc.runGetMethod";
    long id;
    MethodNumber method;
    Deque<TvmStackEntry> stack;
}


