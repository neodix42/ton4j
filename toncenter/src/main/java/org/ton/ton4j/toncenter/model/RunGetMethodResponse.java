package org.ton.ton4j.toncenter.model;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import java.util.List;

/** Response for runGetMethod endpoint */
@Data
public class RunGetMethodResponse {

  @SerializedName("gas_used")
  private Long gasUsed;

  @SerializedName("stack")
  private List<List<Object>> stack;

  @SerializedName("exit_code")
  private Integer exitCode;
}
