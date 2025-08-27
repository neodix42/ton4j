package org.ton.ton4j.adnl.globalconfig;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Builder
@Setter
@Getter
public class LiteServers {
  private long ip;
  private long port;
  private String provided;
  private LiteServerId id;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
