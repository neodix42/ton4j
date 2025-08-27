package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.Map;

/** Response for getLibraries endpoint */
@Data
public class GetLibrariesResponse {
    @SerializedName("libraries")
    private Map<String, String> libraries;
}
