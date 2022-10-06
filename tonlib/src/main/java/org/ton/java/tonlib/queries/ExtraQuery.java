package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class ExtraQuery {
    @SerializedName("@extra")
    public String extra;
}
