package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.ton.java.tonlib.types.Destination;

@Builder
@Setter
@Getter
@ToString
public class CreateQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "raw.createQuery";
    String body; //base64 encoded
    String init_code;
    String init_data;
    Destination destination;
}
