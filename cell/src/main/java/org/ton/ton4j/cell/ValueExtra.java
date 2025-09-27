package org.ton.ton4j.cell;

import lombok.Data;

@Data
public class ValueExtra {
  Object value;
  Object extra;

  public ValueExtra(Object value, Object extra) {
    this.value = value;
    this.extra = extra;
  }
}
