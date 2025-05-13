package org.ton.ton4j.tonlib.types;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

@Getter
@ToString
public class SyncStateInProgress implements Serializable {
    @SerializedName(value = "@type")
    final String type = "syncStateInProgress";
    long from_seqno;
    long to_seqno;
    long current_seqno;
}
