package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Builder
@Setter
@Getter
@ToString
public class TvmStackEntryCell extends TvmStackEntry implements Serializable {
    @SerializedName("@type")
    final String type = "tvm.stackEntryCell";
    TvmCell cell;
}

