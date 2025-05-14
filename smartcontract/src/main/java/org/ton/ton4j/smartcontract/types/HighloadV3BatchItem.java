package org.ton.ton4j.smartcontract.types;

import lombok.Builder;
import lombok.Data;
import org.ton.ton4j.cell.Cell;
import org.ton.ton4j.smartcontract.SendMode;

@Builder
@Data
public class HighloadV3BatchItem {
  @Deprecated(since = "0.9.9", forRemoval = true)
  int mode;

  SendMode sendMode;
  Cell message;
}
