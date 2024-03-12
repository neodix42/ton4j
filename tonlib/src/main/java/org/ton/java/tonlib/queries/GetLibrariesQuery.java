package org.ton.java.tonlib.queries;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@SuperBuilder
@Setter
@Getter
@ToString
public class GetLibrariesQuery extends ExtraQuery {
    @SerializedName(value = "@type")
    final String type = "smc.getLibraries";
    List<String> library_list;
}