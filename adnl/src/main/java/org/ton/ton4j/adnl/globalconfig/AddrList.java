package org.ton.ton4j.adnl.globalconfig;

import com.google.gson.annotations.SerializedName;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Builder
@Setter
@Getter
public class AddrList {
  @SerializedName(value = "@type")
  String type;

  DhtAddr[] addrs;
  long version;
  long reinit_date;
  long priority;
  long expire_at;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
