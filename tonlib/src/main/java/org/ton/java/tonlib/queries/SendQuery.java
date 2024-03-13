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
public class SendQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "query.send";
    long id; // result from createQuery
}
