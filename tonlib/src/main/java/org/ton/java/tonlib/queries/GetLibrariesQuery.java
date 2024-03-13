package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Builder
@Setter
@Getter
@ToString
public class GetLibrariesQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "smc.getLibraries";
    List<String> library_list;
}