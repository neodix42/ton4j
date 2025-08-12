package org.ton.ton4j.adnl.globalconfig;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Builder
@Setter
@Getter
public class BlockInfo {

  private String file_hash;
  private String root_hash;
  private long seqno;
  private long workchain;
  private long shard;

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
  }
}
