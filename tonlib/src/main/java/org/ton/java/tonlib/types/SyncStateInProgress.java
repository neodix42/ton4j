package org.ton.java.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class SyncStateInProgress {
    @SerializedName(value = "@type")
    final String type = "syncStateInProgress";
    long from_seqno;
    long to_seqno;
    long current_seqno;
}
