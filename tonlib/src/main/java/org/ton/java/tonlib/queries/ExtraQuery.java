package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.util.UUID;

@Getter
public class ExtraQuery {
    @SerializedName("@extra")
    public String extra = UUID.randomUUID().toString();
}
